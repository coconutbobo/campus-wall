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
      <label v-if="loggedIn"><input type="checkbox" v-model="subscribedOnly" style="width:auto;" @change="load" /> 只看我订阅的校区</label>
    </div>
    <div class="card" v-if="loggedIn">
      <h3>校区订阅</h3>
      <p>不同校区的信息并不完全互通，订阅后首页可以选择只看订阅校区的内容，例如只看广汉校区的失物招领，或者只看天府校区的二手交易。</p>
      <div v-for="campus in campuses" :key="campus.id">
        <span>{{ campus.name }}</span>
        <button v-if="!isSubscribed(campus.id)" @click="subscribe(campus.id)">订阅</button>
        <button v-else @click="unsubscribe(campus.id)">取消订阅</button>
      </div>
    </div>
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <p v-if="posts.length === 0">还没有帖子，先去发一条吧。</p>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { listCampuses, listPosts, listSubscriptions, subscribeCampus, unsubscribeCampus } from '../api/posts.js'
import PostCard from '../components/PostCard.vue'

const campuses = ref([])
const posts = ref([])
const campusId = ref('')
const category = ref('')
const keyword = ref('')
const subscribedOnly = ref(false)
const subscriptions = ref([])
const loggedIn = ref(!!localStorage.getItem('campus_wall_token'))

function isSubscribed(campusIdValue) {
  return subscriptions.value.some((item) => item.campusId === campusIdValue)
}

async function loadSubscriptions() {
  if (!loggedIn.value) return
  try {
    const response = await listSubscriptions()
    subscriptions.value = response.data
  } catch (e) {
    subscriptions.value = []
  }
}

async function subscribe(campusIdValue) {
  await subscribeCampus(campusIdValue)
  await loadSubscriptions()
}

async function unsubscribe(campusIdValue) {
  await unsubscribeCampus(campusIdValue)
  await loadSubscriptions()
}

async function load() {
  const params = { page: 0, size: 20 }
  if (campusId.value) params.campusId = campusId.value
  if (category.value) params.category = category.value
  if (keyword.value) params.keyword = keyword.value
  if (subscribedOnly.value) params.subscribedOnly = true
  const response = await listPosts(params)
  posts.value = response.data.content || response.data
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  await loadSubscriptions()
  await load()
})

watch([campusId, category], load)
</script>
