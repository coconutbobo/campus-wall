<!-- 校园墙项目：管理后台页，管理员可以在这里删帖、封禁解封账号、处理举报和查看审计日志，普通用户列表里不显示学号。 -->
<template>
  <div>
    <h2>管理后台</h2>
    <p>只有管理员账号可以正常调用下面接口，普通账号会返回无权限。用户列表里不显示学号，查看学号要单独填理由，那次查看会被记进审计日志。</p>
    <div class="card">
      <h3>帖子</h3>
      <div v-for="post in posts" :key="post.id">
        <p>{{ post.title }} · {{ post.displayName }} · {{ post.status }} <button @click="deletePost(post.id)">删除</button></p>
      </div>
    </div>
    <div class="card">
      <h3>用户</h3>
      <div v-for="user in users" :key="user.publicId">
        <p>{{ user.nickname }} · {{ user.status }} · 封禁到：{{ user.banUntil || '永久或未封禁' }} <button @click="ban(user.publicId, 'TEMPORARY')">临时封禁7天</button> <button @click="ban(user.publicId, 'PERMANENT')">永久封禁</button> <button @click="unban(user.publicId)">解封</button> <button @click="viewIdentity(user.publicId)">查看学号</button></p>
      </div>
    </div>
    <div class="card">
      <h3>举报</h3>
      <div v-for="report in reports" :key="report.id">
        <p>{{ report.targetType === 'COMMENT' ? '评论' : '帖子' }}：{{ report.postTitle || report.commentContent }}，原因：{{ report.reason }}，状态：{{ report.status }} <button @click="handle(report.id)">标记已处理</button></p>
      </div>
    </div>
    <div class="card">
      <h3>审计日志</h3>
      <div v-for="log in logs" :key="log.id">
        <p>{{ log.createdAt }} · {{ log.action }} · 目标：{{ log.targetType }} {{ log.targetId }} · 理由：{{ log.reason }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminAuditLogs, adminBanUser, adminDeletePost, adminHandleReport, adminPosts, adminReports, adminUnbanUser, adminUsers, adminViewIdentity } from '../api/posts.js'

const posts = ref([])
const users = ref([])
const reports = ref([])
const logs = ref([])

async function load() {
  try {
    // 管理端接口现在是分页返回，先取 content 字段，兼容旧的数组格式。
    const postsData = (await adminPosts()).data
    posts.value = postsData.content || postsData
    const usersData = (await adminUsers()).data
    users.value = usersData.content || usersData
    const reportsData = (await adminReports()).data
    reports.value = reportsData.content || reportsData
    const logsData = (await adminAuditLogs()).data
    logs.value = logsData.content || logsData
  } catch (e) {
    posts.value = []
    users.value = []
    reports.value = []
    logs.value = []
  }
}

async function deletePost(id) {
  const reason = prompt('请填写删帖理由，理由会记进审计日志')
  await adminDeletePost(id, reason || '未填写理由')
  await load()
}

async function ban(publicId, type) {
  const reason = prompt('请填写封禁理由，理由会记进审计日志')
  await adminBanUser(publicId, { type, days: 7, reason: reason || '未填写理由' })
  await load()
}

async function unban(publicId) {
  const reason = prompt('请填写解封理由')
  await adminUnbanUser(publicId, reason || '未填写理由')
  await load()
}

async function handle(id) {
  const reason = prompt('请填写处理理由')
  await adminHandleReport(id, reason || '未填写理由')
  await load()
}

async function viewIdentity(publicId) {
  const reason = prompt('查看学号必须填写理由，这次查看会被记进审计日志')
  if (!reason) return
  const response = await adminViewIdentity(publicId, reason)
  alert('学号：' + response.data.studentId + '，这次查看已记入审计日志')
}

onMounted(load)
</script>
