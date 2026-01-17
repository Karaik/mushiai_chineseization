<template>
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
    <div
      ref="nodesContainer"
      class="nodes-container"
      :class="{ 'dialogue-active': isDialogueActive }"
    >
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  nodes: {
    type: Array,
    required: true
  },
  links: {
    type: Array,
    required: true
  },
  activeRole: {
    type: String,
    required: true
  },
  completedNodes: {
    type: Set,
    required: true
  },
  isDialogueActive: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['node-hover', 'node-click', 'node-drag']);

const nodesContainer = ref(null);
const isDragging = ref(false);
const activeDragNode = ref(null);

// 获取连线起点
function getLinkStart(link) {
  const [startId] = link;
  const node = props.nodes.find(n => n.id === startId || n.label === startId);
  return node ? { x: node.x, y: node.y } : { x: 0, y: 0 };
}

// 获取连线终点
function getLinkEnd(link) {
  const [, endId] = link;
  const node = props.nodes.find(n => n.id === endId || n.label === endId);
  return node ? { x: node.x, y: node.y } : { x: 0, y: 0 };
}

// 获取节点样式类
function getNodeClasses(node) {
  const classes = ['node-btn'];

  if (node.roles.includes('special')) {
    classes.push('node-special');
  }

  if (props.completedNodes.has(node.id)) {
    classes.push('node-complete');
  }

  // 角色筛选高亮/暗淡
  if (props.activeRole !== 'all') {
    if (node.roles.includes(props.activeRole)) {
      classes.push('highlight');
    } else {
      classes.push('dimmed');
    }
  }

  return classes;
}

// 鼠标悬停
function handleMouseEnter(node) {
  if (!isDragging.value) {
    emit('node-hover', node);
  }
}

function handleMouseLeave() {
  if (!isDragging.value) {
    emit('node-hover', null);
  }
}

// 拖拽逻辑
function handlePointerDown(e, node) {
  if (e.pointerType === 'mouse' && e.button !== 0) return;

  isDragging.value = false;
  const chartMap = nodesContainer.value.parentElement;
  const rect = chartMap.getBoundingClientRect();

  activeDragNode.value = {
    node,
    startX: e.clientX - rect.left,
    startY: e.clientY - rect.top,
    pointerId: e.pointerId,
    chartMap
  };

  document.addEventListener('pointermove', handlePointerMove);
  document.addEventListener('pointerup', handlePointerUp);
  document.addEventListener('pointercancel', handlePointerUp);

  e.target.setPointerCapture?.(e.pointerId);
}

function handlePointerMove(e) {
  if (!activeDragNode.value) return;
  if (activeDragNode.value.pointerId !== null && e.pointerId !== activeDragNode.value.pointerId) return;

  const chartMap = activeDragNode.value.chartMap;
  const rect = chartMap.getBoundingClientRect();
  const currentX = e.clientX - rect.left;
  const currentY = e.clientY - rect.top;

  const dx = currentX - activeDragNode.value.startX;
  const dy = currentY - activeDragNode.value.startY;

  // 移动超过3像素才算拖拽
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    isDragging.value = true;
  }

  if (isDragging.value) {
    const baseWidth = chartMap.offsetWidth;
    const baseHeight = chartMap.offsetHeight;

    if (!baseWidth || !baseHeight) return;

    let newX = (currentX / baseWidth) * 100;
    let newY = (currentY / baseHeight) * 100;

    // 限制边界
    newX = Math.max(0, Math.min(100, newX));
    newY = Math.max(0, Math.min(100, newY));

    emit('node-drag', activeDragNode.value.node.id, newX, newY);

    activeDragNode.value.startX = currentX;
    activeDragNode.value.startY = currentY;
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
    emit('node-click', node);
  }
}
</script>
