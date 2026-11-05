<!-- 校园墙项目：首页：第一行是按时间问候，登录了就带上昵称，下面直接就是最新帖子；筛选和搜索都搬到搜索页了。 -->
<template>
  <div>
    <h2>{{ greeting }}</h2>
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <p v-if="posts.length === 0">还没有帖子，先去发一条吧。</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listPosts } from '../api/posts.js'
import PostCard from '../components/PostCard.vue'

const posts = ref([])
const stored = localStorage.getItem('campus_wall_user')
const user = stored ? JSON.parse(stored) : null

// 按时间问候：0 到 5 点夜深了，5 到 9 点早上好，9 到 12 点上午好，12 到 18 点下午好，18 到 24 点晚上好。
// 登录了就带上昵称，没登录就只说问候。
const greeting = computed(() => {
  const hour = new Date().getHours()
  let word = '晚上好'
  if (hour >= 0 && hour < 5) word = '夜深了'
  else if (hour >= 5 && hour < 9) word = '早上好'
  else if (hour >= 9 && hour < 12) word = '上午好'
  else if (hour >= 12 && hour < 18) word = '下午好'
  else word = '晚上好'
  return user ? `${word}，${user.nickname}` : word
})

async function load() {
  const response = await listPosts({ page: 0, size: 20 })
  posts.value = response.data.content || response.data
}

onMounted(load)
</script>
