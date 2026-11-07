<!-- 校园墙项目：图片查看器：点帖子里的缩略图后全屏显示完整图片，点任意位置关闭。 -->
<template>
  <div class="viewer" @click.stop="$emit('close')">
    <img :src="src" alt="" />
    <span class="hint">点任意位置关闭</span>
  </div>
</template>

<script setup>
defineProps({ src: { type: String, required: true } })
defineEmits(['close'])
</script>

<style scoped>
.viewer { position: fixed; inset: 0; z-index: 100; background: rgba(0, 0, 0, 0.85); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; cursor: pointer; }
.viewer img { max-width: 92vw; max-height: 88vh; object-fit: contain; border-radius: 8px; }
.hint { color: #ddd; font-size: 13px; }
</style>
