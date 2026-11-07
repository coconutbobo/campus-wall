<!-- 校园墙项目：前端外壳：顶部导航栏和页面容器，登录后点昵称会展开下拉菜单，里面是“我的帖子”“我的资料”“我的收藏”，三者平行，退出也在里面。 -->
<!-- 暗色模式存在本地，刷新后还在；颜色都走 CSS 变量，暗色只换变量的值，不用每个页面单独写一套样式。 -->
<!-- 顶栏中间有一条搜索框，宽 500，输完回车或点搜索就跳到搜索页，在那里再筛校区和板块；管理后台只有管理员账号才看得到。 -->
<template>
  <div class="layout">
    <header class="topbar">
      <router-link to="/" class="brand">校园墙</router-link>
      <form class="search-box" @submit.prevent="goSearch">
        <input class="search-input" v-model="searchKeyword" placeholder="搜索帖子" />
        <button type="submit">搜索</button>
      </form>
      <nav>
        <router-link to="/">首页</router-link>
        <router-link to="/publish">发帖</router-link>
        <router-link to="/announcements">公告栏</router-link>
        <router-link v-if="user && user.role === 'ADMIN'" to="/admin">管理后台</router-link>
        <button class="theme-btn" @click="toggleTheme" :title="theme === 'dark' ? '切换到浅色模式' : '切换到暗色模式'">{{ theme === 'dark' ? '☀️' : '🌙' }}</button>
        <router-link v-if="!user" to="/login">登录</router-link>
        <div v-if="user" class="user-menu">
          <button class="nickname-btn" @click.stop="menuOpen = !menuOpen">{{ user.nickname }} ▾</button>
          <div v-if="menuOpen" class="menu">
            <router-link to="/my" @click="menuOpen = false">我的帖子</router-link>
            <router-link to="/profile" @click="menuOpen = false">我的资料</router-link>
            <router-link to="/favorites" @click="menuOpen = false">我的收藏</router-link>
            <button @click="logout">退出</button>
          </div>
        </div>
      </nav>
    </header>
    <main>
      <router-view />
    </main>
    <footer class="site-footer">
      <a href="https://github.com/coconutbobo/campus-wall" target="_blank" rel="noopener">GitHub：源代码</a>
      <span>联系我们：Email：<a href="mailto:wyb13130067185@outlook.com">wyb13130067185@outlook.com</a></span>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout as logoutApi } from './api/posts.js'

const router = useRouter()
const stored = localStorage.getItem('campus_wall_user')
const user = ref(stored ? JSON.parse(stored) : null)
const menuOpen = ref(false)
const searchKeyword = ref('')

// 暗色模式：先读本地存的，没有就默认浅色；切换后立刻存起来，刷新页面也不会丢。
const theme = ref(localStorage.getItem('campus_wall_theme') || 'light')

function applyTheme() {
  document.documentElement.setAttribute('data-theme', theme.value)
  localStorage.setItem('campus_wall_theme', theme.value)
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  applyTheme()
}

// 顶栏搜索：跳到搜索页，把关键词带过去，在那里再筛校区和板块；没输关键词也跳过去，只当浏览全部帖子。
function goSearch() {
  router.push({ path: '/search', query: searchKeyword.value.trim() ? { keyword: searchKeyword.value.trim() } : {} })
}

// 点下拉菜单外面就把它关掉，不然它会一直挡着页面。
function closeMenuOnOutsideClick(event) {
  if (!event.target.closest('.user-menu')) menuOpen.value = false
}

async function logout() {
  menuOpen.value = false
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

onMounted(() => {
  applyTheme()
  document.addEventListener('click', closeMenuOnOutsideClick)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenuOnOutsideClick)
})
</script>

<style>
/* 浅色是默认值，暗色只改下面这些变量的值，页面结构和文字都不用动。 */
:root {
  --bg: #f6f7f9;
  --surface: #fff;
  --text: #222;
  --muted: #666;
  --border: #e5e7eb;
  --input-bg: #fff;
  --input-border: #d1d5db;
  --button-bg: #fff;
  --button-border: #d1d5db;
}
[data-theme="dark"] {
  --bg: #121417;
  --surface: #1e2126;
  --text: #e5e7eb;
  --muted: #9ca3af;
  --border: #2e3238;
  --input-bg: #262a30;
  --input-border: #3a3f47;
  --button-bg: #262a30;
  --button-border: #3a3f47;
}
body { margin: 0; font-family: system-ui, sans-serif; background: var(--bg); color: var(--text); }
.topbar { display: flex; align-items: center; gap: 14px; padding: 14px 20px; background: var(--surface); border-bottom: 1px solid var(--border); }
.topbar nav { display: flex; gap: 14px; align-items: center; margin-left: auto; }
.topbar a { color: var(--text); text-decoration: none; }
.brand { font-weight: 700; font-size: 18px; }
main { max-width: 880px; margin: 0 auto; padding: 20px; }
/* 每个页面底下的页脚：GitHub 源代码链接和联系邮箱，所有页面共用这一个。 */
.site-footer { max-width: 880px; margin: 0 auto; padding: 20px; display: flex; flex-wrap: wrap; gap: 8px 20px; justify-content: center; color: var(--muted); border-top: 1px solid var(--border); font-size: 14px; }
.site-footer a { color: var(--muted); }
button { cursor: pointer; background: var(--button-bg); color: var(--text); border: 1px solid var(--button-border); border-radius: 6px; padding: 6px 10px; }
input, select, textarea { width: 100%; padding: 8px; margin: 6px 0 12px; box-sizing: border-box; background: var(--input-bg); color: var(--text); border: 1px solid var(--input-border); border-radius: 6px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 16px; margin-bottom: 14px; }
/* 顶栏搜索框：放在顶栏中间，输入框宽 500，不走上面 input 那条 100% 宽的通用样式。 */
.search-box { flex: 1; display: flex; gap: 6px; align-items: center; justify-content: center; }
.search-input { width: 500px; max-width: 100%; margin: 0; }
/* 昵称下拉菜单：点昵称才展开，里面的“我的帖子”“我的资料”“我的收藏”是平行的三个入口，退出也在同一层。 */
.user-menu { position: relative; }
.nickname-btn { border: none; background: transparent; font-size: 15px; padding: 6px 4px; }
.theme-btn { border: none; background: transparent; font-size: 16px; padding: 6px 4px; }
.menu { position: absolute; right: 0; top: 100%; min-width: 140px; background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 6px; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15); z-index: 10; }
.menu a, .menu button { display: block; width: 100%; text-align: left; padding: 8px 12px; border: none; background: transparent; box-sizing: border-box; }
.menu a:hover, .menu button:hover { background: var(--bg); border-radius: 6px; }
</style>
