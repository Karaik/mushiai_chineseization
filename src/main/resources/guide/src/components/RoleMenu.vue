<template>
  <aside class="role-sidebar">
    <div class="sidebar-header">STAFF ROLL</div>
    <div id="roleMenu" class="role-list">
      <div
        v-for="role in roles"
        :key="role.id"
        :class="['role-btn', role.style, { active: activeRole === role.id }]"
        @click="handleRoleClick(role)"
      >
        {{ role.label }}
      </div>
    </div>
    <div class="sidebar-footer">
      <a
        class="open-source-badge open-source-badge--sidebar"
        href="https://github.com/Karaik/mushiai_chineseization"
        target="_blank"
        rel="noopener noreferrer"
      >
        本项目已开源
      </a>
    </div>
  </aside>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  roles: {
    type: Array,
    required: true
  },
  activeRole: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['role-change']);

function handleRoleClick(role) {
  const isActive = props.activeRole === role.id;

  // 如果点击的是已激活的非"全部显示"角色，则切换回"全部显示"
  if (isActive && role.id !== 'all') {
    emit('role-change', 'all');
    return;
  }

  // 如果点击的是已激活的"全部显示"，则不做任何操作
  if (isActive && role.id === 'all') {
    return;
  }

  // 否则切换到新角色
  emit('role-change', role.id);
}
</script>
