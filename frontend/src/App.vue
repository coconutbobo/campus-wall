<!-- 校园墙项目：前端外壳：顶部导航栏和页面容器，登录后显示昵称，退出时清除本地令牌和用户信息。 -->
<template>
  <div class="layout">
    <header class="topbar">
      <router-link to="/" class="brand">校园墙</router-link>
      <nav>
        <router-link to="/">首页</router-link>
        <router-link to="/publish">发帖</router-link>
        <router-link v-if="user" to="/my">我的帖子</router-link>
        <router-link to="/admin">管理后台</router-link>
        <span v-if="user">{{ user.nickname }}</span>
        <router-link v-if="!user" to="/login">登录</router-link>
        <button v-if="user" @click="logout">退出</button>
      </nav>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from './api/posts.js'

const router = useRouter()
const stored = localStorage.getItem('campus_wall_user')
const user = ref(stored ? JSON.parse(stored) : null)

async function logout() {
  // 先调服务端注销接口，让旧令牌立刻失效，只清本地令牌是不够的，旧令牌在有效期内仍然能用。
  try {
    await logoutApi()
  } catch (e) {
  }
  localStorage.removeItem('campus_wall_token')
  localStorage.removeItem('campus_wall_user')
  user.value = null
  router.push('/login')
}
</script>

<style>
body { margin: 0; font-family: system-ui, sans-serif; background: #f6f7f9; color: #222; }
.topbar { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; background: #fff; border-bottom: 1px solid #e5e7eb; }
.topbar nav { display: flex; gap: 14px; align-items: center; }
.topbar a { color: #222; text-decoration: none; }
.brand { font-weight: 700; font-size: 18px; }
main { max-width: 880px; margin: 0 auto; padding: 20px; }
button { cursor: pointer; }
input, select, textarea { width: 100%; padding: 8px; margin: 6px 0 12px; box-sizing: border-box; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 16px; margin-bottom: 14px; }
</style>
