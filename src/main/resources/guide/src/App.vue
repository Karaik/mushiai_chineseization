<template>
  <!-- 遮罩层 - 与原始代码完全一致，在 body 下 -->
  <div class="bg-overlay"></div>

  <main class="container">
    <header class="top-bar">
      <div class="logo-area">
        <h1>甜心汉化组&蟲爱少女填坑组 <small>汉化感言</small></h1>
      </div>
    </header>

    <!-- 视图 A: 地图模式 -->
    <section id="viewMap" class="stage-map fade-in">
      <RoleMenu
        :roles="roles"
        :activeRole="activeRole"
        @role-change="handleRoleChange"
      />

      <div class="map-content">
        <div class="hover-info-panel">
          <div class="episode-roles" id="hoverRoles">{{ hoverInfo.roles }}</div>
          <h2 class="episode-title" id="hoverTitle">{{ hoverInfo.title }}</h2>
          <p class="episode-desc" id="hoverDesc">{{ hoverInfo.desc }}</p>
        </div>

        <CompletionPanel :completed="completedNodes.size" :total="nodes.length" />

        <div class="chart-map">
          <div class="map-grid-bg"></div>
          <svg class="connections" id="linesLayer" width="100%" height="100%">
            <line
              v-for="(link, index) in links"
              :key="index"
              :x1="getLinkStart(link).x + '%'"
              :y1="getLinkStart(link).y + '%'"
              :x2="getLinkEnd(link).x + '%'"
              :y2="getLinkEnd(link).y + '%'"
              stroke="#6fb6e8"
              stroke-width="2"
              opacity="0.65"
            />
          </svg>
          <div id="nodesLayer" class="nodes-container" :class="{ 'dialogue-active': isDialogueActive }">
            <!-- 节点 -->
            <div
              v-for="node in nodes"
              :key="node.id"
              :class="getNodeClasses(node)"
              :style="{ left: node.x + '%', top: node.y + '%' }"
              :data-roles="JSON.stringify(node.roles)"
              @mouseenter="handleMouseEnter(node)"
              @mouseleave="handleMouseLeave()"
              @pointerdown="handlePointerDown($event, node)"
              @click="handleClick(node)"
            >
              {{ node.label || node.id }}
            </div>

            <!-- 视图 B: 对话模式（仅覆盖 nodes-container） - 与原始代码完全一致 -->
            <DialogueBox
              v-if="isDialogueActive"
              :dialogueData="currentDialogue"
              :isEasterEgg="isEasterEgg"
              @next="handleDialogueNext"
              @close="handleDialogueClose"
            />
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import RoleMenu from './components/RoleMenu.vue';
import DialogueBox from './components/DialogueBox.vue';
import CompletionPanel from './components/CompletionPanel.vue';
import { parseCSV } from './utils/csvParser';
import { ROLES, NODES, LINKS } from './data/constants';
import csvContent from '../data.inline.csv?raw';

// 状态管理
const activeRole = ref('all');
const isDialogueActive = ref(false);
const isEasterEgg = ref(false);
const completedNodes = reactive(new Set());
const currentDialogue = ref(null);
const roles = ROLES;
const nodes = reactive(NODES);
const links = LINKS;

const hoverInfo = reactive({
  roles: '甜心汉化组&蟲爱少女填坑组',
  title: '汉化感言',
  desc: '请点击对应汉化人员节点查看汉化感言哦~'
});

// 拖拽状态
const isDragging = ref(false);
const activeDragNode = ref(null);

// 初始化
onMounted(() => {
  const isMobileUa = /Android|iPhone|iPad|iPod|Windows Phone/i.test(navigator.userAgent);
  if (isMobileUa) {
    alert('请在电脑端访问，体验更佳哦~');
    const viewport = document.querySelector('meta[name="viewport"]');
    if (viewport) {
      viewport.setAttribute('content', 'width=1280, initial-scale=1.0');
    }
    document.body.classList.add('force-desktop');
    document.body.classList.add('force-landscape');
  }

  // 解析 CSV 数据并填充到节点
  const csvData = parseCSV(csvContent);
  nodes.forEach(node => {
    const lookupKey = node.label || node.id;
    const data = csvData.get(lookupKey);
    if (data) {
      node.runtimeEp = data.ep;
      node.runtimeContent = data.content;
      node.runtimeAvatar = data.avatar;
    }
  });
});

// 获取连线起点
function getLinkStart(link) {
  const [startId] = link;
  const node = nodes.find(n => n.id === startId || n.label === startId);
  return node ? { x: node.x, y: node.y } : { x: 0, y: 0 };
}

// 获取连线终点
function getLinkEnd(link) {
  const [, endId] = link;
  const node = nodes.find(n => n.id === endId || n.label === endId);
  return node ? { x: node.x, y: node.y } : { x: 0, y: 0 };
}

// 获取节点样式类
function getNodeClasses(node) {
  const classes = ['node-btn'];

  if (node.roles.includes('special')) {
    classes.push('node-special');
  }

  if (completedNodes.has(node.id)) {
    classes.push('node-complete');
  }

  // 角色筛选高亮/暗淡
  if (activeRole.value !== 'all') {
    if (node.roles.includes(activeRole.value)) {
      classes.push('highlight');
    } else {
      classes.push('dimmed');
    }
  }

  return classes;
}

// 事件处理
function handleRoleChange(roleId) {
  activeRole.value = roleId;
}

// 更新 hover 信息的公共函数
function updateHoverInfo(node) {
  const lookupKey = node.label || node.id;
  hoverInfo.title = lookupKey;
  hoverInfo.desc = node.runtimeEp || '测试ep文本';

  const roleLabels = node.roles
    .filter(r => r !== 'special' && r !== 'egg')
    .map(rId => {
      const r = ROLES.find(item => item.id === rId);
      return r ? r.label : '';
    })
    .filter(l => l).join(' / ');

  // 彩蛋节点显示特殊文本
  if (node.roles.includes('egg')) {
    hoverInfo.roles = '？？？？？？？？？？？？？？';
  } else if (roleLabels) {
    hoverInfo.roles = roleLabels;
  } else if (node.roles.includes('special')) {
    hoverInfo.roles = '特别致谢';
  } else {
    hoverInfo.roles = '';
  }
}

function handleMouseEnter(node) {
  // 如果正在对话中，不更新 hover 信息
  if (!isDragging.value && !isDialogueActive.value) {
    updateHoverInfo(node);
  }
}

function handleMouseLeave() {
  // 如果正在对话中，不重置 hover 信息
  if (!isDragging.value && !isDialogueActive.value) {
    hoverInfo.roles = '甜心汉化组&蟲爱少女填坑组';
    hoverInfo.title = '汉化感言';
    hoverInfo.desc = '请点击对应汉化人员节点查看汉化感言哦~';
  }
}

// 拖拽逻辑
function handlePointerDown(e, node) {
  if (e.pointerType === 'mouse' && e.button !== 0) return;

  isDragging.value = false;
  const chartMap = document.querySelector('.chart-map');
  if (!chartMap) return;

  const start = getDragPoint(e.clientX, e.clientY, chartMap);

  activeDragNode.value = {
    node,
    startX: start.x,
    startY: start.y,
    pointerId: e.pointerId,
    pointerType: e.pointerType,
    chartMap
  };

  document.addEventListener('pointermove', handlePointerMove);
  document.addEventListener('pointerup', handlePointerUp);
  document.addEventListener('pointercancel', handlePointerUp);

  e.target.setPointerCapture?.(e.pointerId);
}

// 获取拖拽点坐标 - 处理横屏模式
function getDragPoint(clientX, clientY, chartMap) {
  if (!document.body.classList.contains('force-landscape')) {
    const rect = chartMap.getBoundingClientRect();
    return { x: clientX - rect.left, y: clientY - rect.top };
  }

  // 横屏模式：需要坐标转换
  const container = document.querySelector('.container');
  const rect = container.getBoundingClientRect();
  const sx = clientX - rect.left;
  const sy = clientY - rect.top;
  const containerHeight = container.offsetHeight;
  const localX = sy;
  const localY = containerHeight - sx;

  // 使用 offsetLeft/offsetTop 计算偏移（与原始代码一致）
  const offset = getOffsetToContainer(chartMap, container);
  return { x: localX - offset.x, y: localY - offset.y };
}

// 计算元素相对于 container 的偏移
function getOffsetToContainer(el, container) {
  let x = 0;
  let y = 0;
  let current = el;
  while (current && current !== container) {
    x += current.offsetLeft || 0;
    y += current.offsetTop || 0;
    current = current.offsetParent;
  }
  return { x, y };
}

function handlePointerMove(e) {
  if (!activeDragNode.value) return;
  if (activeDragNode.value.pointerId !== null && e.pointerId !== activeDragNode.value.pointerId) return;

  // 触摸拖拽时阻止默认行为（防止页面滚动）
  if (activeDragNode.value.pointerType === 'touch' && isDragging.value) {
    e.preventDefault();
  }

  const chartMap = activeDragNode.value.chartMap;
  const point = getDragPoint(e.clientX, e.clientY, chartMap);

  const dx = point.x - activeDragNode.value.startX;
  const dy = point.y - activeDragNode.value.startY;

  // 移动超过3像素才算拖拽
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    isDragging.value = true;
  }

  if (isDragging.value) {
    const baseWidth = chartMap.offsetWidth;
    const baseHeight = chartMap.offsetHeight;

    if (!baseWidth || !baseHeight) return;

    let newX = (point.x / baseWidth) * 100;
    let newY = (point.y / baseHeight) * 100;

    // 限制边界
    newX = Math.max(0, Math.min(100, newX));
    newY = Math.max(0, Math.min(100, newY));

    activeDragNode.value.node.x = newX;
    activeDragNode.value.node.y = newY;
  }
}

function handlePointerUp(e) {
  if (!activeDragNode.value) return;
  if (activeDragNode.value.pointerId !== null && e.pointerId !== activeDragNode.value.pointerId) return;

  activeDragNode.value = null;
  document.removeEventListener('pointermove', handlePointerMove);
  document.removeEventListener('pointerup', handlePointerUp);
  document.removeEventListener('pointercancel', handlePointerUp);

  setTimeout(() => {
    isDragging.value = false;
  }, 0);
}

function handleClick(node) {
  if (!isDragging.value) {
    const lookupKey = node.label || node.id;
    let rawContent = node.runtimeContent;

    if (!rawContent) {
      if (node.id === 'egg') rawContent = "🎉🎉🎉🎉🎉";
      else rawContent = "暂无内容...@p请配置文本。";
    }

    const dialogLines = rawContent.split('@p').map(page => page.replace(/@n/g, '<br>'));

    currentDialogue.value = {
      name: lookupKey,
      lines: dialogLines,
      currentIndex: 0,
      avatar: node.runtimeAvatar || '',
      nodeId: node.id
    };

    // 设置 episode 信息并在对话期间保持
    updateHoverInfo(node);

    isEasterEgg.value = node.id === 'egg';
    isDialogueActive.value = true;

    if (isEasterEgg.value) {
      document.body.classList.add('easter-active');
    }
  }
}

function handleDialogueNext() {
  if (!currentDialogue.value) return;

  currentDialogue.value.currentIndex++;

  if (currentDialogue.value.currentIndex >= currentDialogue.value.lines.length) {
    handleDialogueClose();
  }
}

function handleDialogueClose() {
  if (currentDialogue.value) {
    const nodeId = currentDialogue.value.nodeId;
    completedNodes.add(nodeId);
  }

  isDialogueActive.value = false;
  isEasterEgg.value = false;
  currentDialogue.value = null;

  // 重置 episode 信息为默认状态
  hoverInfo.roles = '甜心汉化组&蟲爱少女填坑组';
  hoverInfo.title = '汉化感言';
  hoverInfo.desc = '请点击对应汉化人员节点查看汉化感言哦~';

  // 注意：原始代码中不移除 easter-active 类，背景会永久保持彩蛋图片
  // document.body.classList.remove('easter-active');
}
</script>

<style>
@import '../css/css.css';

/* 确保 Vue 根元素充满整个视口，让遮罩层能正确覆盖 */
#app {
  position: relative;
  width: 100%;
  min-height: 100vh;
}
</style>
