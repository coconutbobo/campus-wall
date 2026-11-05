<!-- 校园墙项目：公告栏：和首页、发帖平行的一级页面，所有人都能看；只有管理员能发和删公告。 -->
<template>
  <div>
    <h2>公告栏</h2>
    <div class="card" v-if="isAdmin">
      <h3>发公告</h3>
      <label>标题</label>
      <input v-model="title" />
      <label>内容</label>
      <textarea v-model="content" rows="4"></textarea>
      <button @click="publish">发布公告</button>
      <p>{{ message }}</p>
    </div>
    <div class="card" v-for="item in announcements" :key="item.id">
      <h3>{{ item.title }}</h3>
      <p>{{ item.content }}</p>
      <p>{{ item.createdAt }}</p>
      <button v-if="isAdmin" @click="remove(item.id)">删除</button>
    </div>
    <p v-if="announcements.length === 0">还没有公告。</p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { createAnnouncement, deleteAnnouncement, listAnnouncements } from '../api/posts.js'

const announcements = ref([])
const title = ref('')
const content = ref('')
const message = ref('')
const stored = localStorage.getItem('campus_wall_user')
const user = stored ? JSON.parse(stored) : null
const isAdmin = ref(!!user && user.role === 'ADMIN')

async function load() {
  try {
    const response = await listAnnouncements()
    announcements.value = response.data
  } catch (e) {
    announcements.value = []
  }
}

async function publish() {
  if (!title.value.trim() || !content.value.trim()) {
    message.value = '标题和内容都要填'
    return
  }
  try {
    await createAnnouncement({ title: title.value, content: content.value })
    title.value = ''
    content.value = ''
    message.value = ''
    await load()
  } catch (e) {
    message.value = '发布失败，只有管理员能发公告'
  }
}

async function remove(id) {
  if (!confirm('确定删除这条公告吗？')) return
  await deleteAnnouncement(id)
  await load()
}

onMounted(load)
</script>
