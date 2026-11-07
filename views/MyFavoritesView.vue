<!-- 校园墙项目：我的收藏页：只列出当前用户收藏过的帖子，和“我的帖子”平行，入口在昵称下拉菜单里。 -->
<template>
  <div>
    <h2>我的收藏</h2>
    <p>这里是你收藏过的帖子，点进详情页再点一次收藏就能取消收藏。</p>
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <p v-if="posts.length === 0">你还没有收藏过帖子。</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myFavorites } from '../api/posts.js'
import PostCard from '../components/PostCard.vue'

const posts = ref([])

async function load() {
  try {
    const data = (await myFavorites()).data
    posts.value = data.content || data
  } catch (e) {
    posts.value = []
  }
}

onMounted(load)
</script>
