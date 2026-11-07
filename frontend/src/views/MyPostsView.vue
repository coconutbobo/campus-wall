<!-- 校园墙项目：我的帖子页，本人能看到自己的匿名帖子，也能从这里编辑和删除，别人看不到这个页面里的本人标记。 -->
<!-- 我的资料已经搬到单独的“我的资料”页（/profile）了，这里只留帖子，和“我的资料”平行。 -->
<template>
  <div>
    <h2>我的帖子</h2>
  
    <div class="card" v-for="post in posts" :key="post.id">
      <h3>{{ post.title }}</h3>
      <p>{{ post.content }}</p>
      <p>展示：{{ post.anonymous ? '匿名用户' : post.displayName }} · 状态：{{ postStatusText(post) }}</p>
      <button @click="router.push('/post/' + post.id + '/edit')">编辑</button>
      <button @click="remove(post.id)">删除</button>
      <button @click="goDetail(post.id)">查看详情</button>
    </div>
    <p v-if="posts.length === 0">你还没有发过帖子。</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteOwnPost, myPosts } from '../api/posts.js'
import { postStatusText } from '../utils/statusText.js'

const router = useRouter()
const posts = ref([])

async function load() {
  try {
    // 我的帖子接口现在是分页返回，先取 content 字段。
    const data = (await myPosts()).data
    posts.value = data.content || data
  } catch (e) {
    posts.value = []
  }
}

function goDetail(id) {
  router.push('/post/' + id)
}

async function remove(id) {
  if (!confirm('确定删除这条帖子吗？')) return
  await deleteOwnPost(id)
  await load()
}

onMounted(load)
</script>
