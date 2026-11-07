<!-- 校园墙项目：帖子卡片组件：首页列表里每一条帖子的展示方式，包括板块、校区、作者、价格地点等关键信息，点击进入详情。 -->
<template>
  <div class="card" @click="goDetail">
    <div class="meta">
      <span>{{ categoryLabel }}</span>
      <span>{{ post.campusName }}</span>
      <span>{{ post.displayName }}</span>
    </div>
    <h3>{{ post.title }}</h3>
    <p>{{ post.content }}</p>
    <div v-if="post.price">价格：¥{{ post.price }}，成色：{{ post.conditionText }}</div>
    <div v-if="post.location">地点：{{ post.location }}</div>
    <div v-if="post.solved !== undefined">状态：{{ post.solved ? '已解决' : '未解决' }}</div>
    <div class="images" v-if="post.images && post.images.length">
      <img v-for="url in post.images" :key="url" :src="url" alt="" @click.stop="viewerUrl = url" />
    </div>
    <ImageViewer v-if="viewerUrl" :src="viewerUrl" @close="viewerUrl = null" />
    <div class="meta">
      <span>点赞 {{ post.likeCount }}</span>
      <span>收藏 {{ post.favoriteCount }}</span>
      <span>{{ post.createdAt }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import ImageViewer from './ImageViewer.vue'

const props = defineProps({ post: { type: Object, required: true } })
const router = useRouter()

// 正在查看的完整图片地址，有值就弹出查看器，关掉就清空。
const viewerUrl = ref(null)

const labels = { LOST: '失物招领', SECONDHAND: '二手交易', QA: '提问求助', VENT: '喝茶闲聊' }
const categoryLabel = computed(() => labels[props.post.category] || props.post.category)

function goDetail() {
  router.push('/post/' + props.post.id)
}
</script>

<style scoped>
.card { cursor: pointer; }
.meta { display: flex; gap: 10px; color: var(--muted, #666); font-size: 13px; }
.images img { width: 120px; height: 120px; object-fit: cover; margin-right: 8px; border-radius: 8px; cursor: zoom-in; }
</style>
