<!-- 校园墙项目：搜索页：从顶栏搜索框跳过来，关键词在地址栏里，到了这里再筛校区和板块，也能只看订阅的校区。 -->
<template>
  <div>
    <h2>搜索</h2>
    <div class="card">
      <label>关键词</label>
      <input v-model="keyword" placeholder="搜标题或内容" @keyup.enter="load" />
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
        <option value="VENT">喝茶闲聊</option>
      </select>
      <button @click="load">搜索</button>
      <label v-if="loggedIn"><input type="checkbox" v-model="subscribedOnly" style="width:auto;" @change="load" /> 只看我订阅的校区</label>
    </div>
    <div class="card" v-if="loggedIn">
      <h3>校区订阅</h3>
      <p>不同校区的信息并不完全互通，订阅后在这里可以选择只看订阅校区的内容，例如只看广汉校区的失物招领，或者只看天府校区的二手交易。</p>
      <div v-for="campus in campuses" :key="campus.id">
        <span>{{ campus.name }}</span>
        <button v-if="!isSubscribed(campus.id)" @click="subscribe(campus.id)">订阅</button>
        <button v-else @click="unsubscribe(campus.id)">取消订阅</button>
      </div>
    </div>
    <PostCard v-for="post in posts" :key="post.id" :post="post" />
    <p v-if="posts.length === 0">没有找到相关帖子，换个关键词或放宽筛选试试。</p>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listCampuses, listPosts, listSubscriptions, subscribeCampus, unsubscribeCampus } from '../api/posts.js'
import PostCard from '../components/PostCard.vue'

const route = useRoute()
const campuses = ref([])
const posts = ref([])
const campusId = ref('')
const category = ref('')
const keyword = ref(route.query.keyword || '')
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
watch(() => route.query.keyword, (value) => {
  keyword.value = value || ''
  load()
})
</script>
