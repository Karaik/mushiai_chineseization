<template>
  <div class="completion-panel" id="completionPanel">
    <span class="completion-value completion-value-top" id="completionValueTop">{{ percent }}</span>
    <span class="completion-value completion-value-bottom" id="completionValueBottom">{{ percent }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  completed: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  }
});

const percent = computed(() => {
  return Math.floor((props.completed / props.total) * 100);
});
</script>
