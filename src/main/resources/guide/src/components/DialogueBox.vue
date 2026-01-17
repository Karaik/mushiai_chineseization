<template>
  <section
    class="stage-dialogue"
    @click="handleBackgroundClick"
  >
    <!-- 彩蛋图片 -->
    <div
      v-if="isEasterEgg"
      class="dialogue-art"
      :class="{ zoomed: isZoomed }"
      @click.stop="toggleZoom"
    >
      <img :src="easterArtUrl" :alt="dialogueData.name" />
    </div>

    <!-- 对话框 -->
    <div class="dialogue-box" @click.stop="handleNext">
      <!-- 角色头像 -->
      <div v-if="avatarUrl" class="character-stand" :style="avatarStyle">
        <img :src="avatarUrl" :alt="dialogueData.name" @error="handleAvatarError" />
      </div>

      <!-- 角色名 -->
      <div class="name-tag">{{ dialogueData.name }}</div>

      <!-- 对话文本 -->
      <div class="text-content" v-html="currentLine"></div>

      <!-- 下一步箭头 -->
      <div class="dialog-arrow">▼</div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import easterArtUrl from '../../images/egg.webp';

const props = defineProps({
  dialogueData: {
    type: Object,
    required: true
  },
  isEasterEgg: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['next', 'close']);

const isZoomed = ref(false);
const avatarError = ref(false);
const avatarStyle = ref({});

// 导入头像资源
const avatarAssets = import.meta.glob('../../images/head/*.webp', { eager: true, import: 'default' });
const avatarMap = new Map(
  Object.entries(avatarAssets).map(([filePath, url]) => [filePath.split('/').pop(), url])
);

// 当前显示的对话行
const currentLine = computed(() => {
  if (!props.dialogueData || !props.dialogueData.lines) return '';
  const index = props.dialogueData.currentIndex;
  return props.dialogueData.lines[index] || '';
});

// 头像 URL - 与原始代码逻辑完全一致
const avatarUrl = computed(() => {
  if (avatarError.value) return null;
  if (!props.dialogueData) return null;

  // 原始逻辑：如果没有头像，使用默认头像
  const avatarRaw = props.dialogueData.avatar ? props.dialogueData.avatar.trim() : '';
  const avatarKey = avatarRaw !== '' ? avatarRaw : '默认头像.webp';
  return avatarMap.get(avatarKey) || `images/head/${avatarKey}`;
});

// 切换图片缩放
function toggleZoom() {
  isZoomed.value = !isZoomed.value;
}

// 处理头像加载错误
function handleAvatarError() {
  avatarError.value = true;
}

// 点击下一步
function handleNext(e) {
  // 防止文本选中
  e.preventDefault();
  emit('next');
}

// 点击背景关闭
function handleBackgroundClick() {
  emit('next');
}

// 定位头像
function positionAvatar() {
  if (!avatarUrl.value) return;

  nextTick(() => {
    const dialogBox = document.querySelector('.dialogue-box');
    const nameTag = document.querySelector('.name-tag');

    if (!dialogBox || !nameTag) return;

    const boxRect = dialogBox.getBoundingClientRect();
    const tagRect = nameTag.getBoundingClientRect();

    const centerX = tagRect.left - boxRect.left + tagRect.width / 2;
    const topY = tagRect.top - boxRect.top;

    avatarStyle.value = {
      left: `${centerX}px`,
      top: `${topY}px`,
      display: 'block'
    };
  });
}

onMounted(() => {
  positionAvatar();
  window.addEventListener('resize', positionAvatar);
});

// 监听 dialogueData 变化，重新定位头像
watch(() => props.dialogueData, () => {
  positionAvatar();
}, { deep: true });
</script>

<style scoped>
/* 防止文本选中 */
.text-content {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.dialogue-box {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.character-stand {
  display: none;
}
</style>
