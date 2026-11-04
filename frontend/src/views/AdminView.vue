<!-- 校园墙项目：管理后台页：展示帖子、用户和举报列表，管理员可以在这里删帖、封禁解封账号和标记举报已处理。 -->
<template>
  <div>
    <h2>管理后台</h2>
    <p>只有管理员账号可以正常调用下面接口，普通账号会返回无权限。</p>
    <div class="card">
      <h3>帖子</h3>
      <div v-for="post in posts" :key="post.id">
        <p>{{ post.title }} · {{ post.displayName }} · {{ post.status }} <button @click="deletePost(post.id)">删除</button></p>
      </div>
    </div>
    <div class="card">
      <h3>用户</h3>
      <div v-for="user in users" :key="user.id">
        <p>{{ user.nickname }} · {{ user.studentId }} · {{ user.status }} <button @click="ban(user.id)">封禁</button> <button @click="unban(user.id)">解封</button></p>
      </div>
    </div>
    <div class="card">
      <h3>举报</h3>
      <div v-for="report in reports" :key="report.id">
        <p>帖子：{{ report.postTitle }}，原因：{{ report.reason }}，状态：{{ report.status }} <button @click="handle(report.id)">标记已处理</button></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminBanUser, adminDeletePost, adminHandleReport, adminPosts, adminReports, adminUnbanUser, adminUsers } from '../api/posts.js'

const posts = ref([])
const users = ref([])
const reports = ref([])

async function load() {
  try {
    posts.value = (await adminPosts()).data
    users.value = (await adminUsers()).data
    reports.value = (await adminReports()).data
  } catch (e) {
    posts.value = []
    users.value = []
    reports.value = []
  }
}

async function deletePost(id) {
  await adminDeletePost(id)
  await load()
}

async function ban(id) {
  await adminBanUser(id)
  await load()
}

async function unban(id) {
  await adminUnbanUser(id)
  await load()
}

async function handle(id) {
  await adminHandleReport(id)
  await load()
}

onMounted(load)
</script>
