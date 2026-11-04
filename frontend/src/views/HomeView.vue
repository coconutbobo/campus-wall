<!-- 校园墙项目：首页：按校区和板块筛选帖子，支持关键词搜索，不登录也能看帖子列表。 -->
<template>
  <div>
    <h2>校园墙</h2>
    <div class="card">
      <label>校区</label>
      <select v-model="campusId">
        <option value="">全部校区</option>
        <option v-for="campus in campuses" :key="campus.id" :value="campus.id">{{ campus.name }}</option>
      </select>
      <label>板块</label>
      <select v-model="category">
        <option value="">全部板块</option>
        <option value="LOST">失物招领</option>
        <option value="SECONDHAND">二手交易</option>
        <option value="QA">提问求助</option>
        <option value="VENT">吐槽</option>
      </select>
      <label>搜索</label>
      <input v-model="keyword" placeholder="搜标题或内容" @keyup.enter="load" />
      <button @click="load">搜索</button>
    </div>
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <p v-if="posts.length === 0">还没有帖子，先去发一条吧。</p>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { listCampuses, listPosts } from '../api/posts.js'
import PostCard from '../components/PostCard.vue'

const campuses = ref([])
const posts = ref([])
const campusId = ref('')
const category = ref('')
const keyword = ref('')

async function load() {
  const params = { page: 0, size: 20 }
  if (campusId.value) params.campusId = campusId.value
  if (category.value) params.category = category.value
  if (keyword.value) params.keyword = keyword.value
  const response = await listPosts(params)
  posts.value = response.data.content || response.data
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  await load()
})

watch([campusId, category], load)
</script>
