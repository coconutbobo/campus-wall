<!-- 校园墙项目：我的帖子页，本人能看到自己的匿名帖子，也能从这里编辑和删除，别人看不到这个页面里的本人标记。 -->
<template>
  <div>
    <h2>我的帖子</h2>
    <p>匿名帖子在这里会标注仅你可见这是你的帖子，别人只会看到匿名用户，你不会因为匿名就失去管理权。</p>
    <div class="card">
      <h3>我的资料</h3>
      <p>学号注册后就不能改了，这里只能改昵称；昵称 30 天只能改一次，改完要等满 30 天才能再改。</p>
      <label>昵称</label>
      <input v-model="nickname" maxlength="64" />
      <p v-if="nextNicknameChangeAt">下次可改昵称：{{ nextNicknameChangeAt }}</p>
      <p v-else>现在可以改昵称。</p>
      <button @click="saveProfile">保存资料</button>
      <p>{{ profileMessage }}</p>
    </div>
    <div class="card">
      <h3>更改密码</h3>
      <p>改密码要先输对原密码，改完要重新登录一次。</p>
      <label>原密码</label>
      <input type="password" v-model="currentPassword" />
      <label>新密码</label>
      <input type="password" v-model="newPassword" />
      <label>确认新密码</label>
      <input type="password" v-model="confirmPassword" />
      <button @click="savePassword">更改密码</button>
      <p>{{ passwordMessage }}</p>
    </div>
    <div class="card" v-for="post in posts" :key="post.id">
      <h3>{{ post.title }}</h3>
      <p>{{ post.content }}</p>
      <p>展示：{{ post.anonymous ? '匿名用户' : post.displayName }} · 状态：{{ post.status }}</p>
      <button @click="edit(post)">编辑</button>
      <button @click="remove(post.id)">删除</button>
      <button @click="goDetail(post.id)">查看详情</button>
    </div>
    <p v-if="posts.length === 0">你还没有发过帖子。</p>
    <div class="card">
      <h3>注销账号</h3>
      <p>注销后你的学号会被清除，密码会被清空，你的帖子和评论会改为匿名展示，这个操作不可恢复。</p>
      <button @click="deactivate">注销我的账号</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, deleteAccount, deleteOwnPost, editPost, getProfile, myPosts, updateProfile } from '../api/posts.js'

const router = useRouter()
const posts = ref([])
const nickname = ref('')
const nextNicknameChangeAt = ref('')
const profileMessage = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordMessage = ref('')

async function load() {
  try {
    // 我的帖子接口现在是分页返回，先取 content 字段。
    const data = (await myPosts()).data
    posts.value = data.content || data
  } catch (e) {
    posts.value = []
  }
  try {
    // 资料接口不返回学号，所以这里只有昵称和下次可改日期，学号根本不显示、也不让改。
    const profile = (await getProfile()).data
    nickname.value = profile.nickname || ''
    nextNicknameChangeAt.value = profile.nextNicknameChangeAt || ''
  } catch (e) {
  }
}

async function saveProfile() {
  try {
    const response = await updateProfile({ nickname: nickname.value })
    // 顶栏的昵称是登录时存下来的，改完只刷新页面不会变，先同步本地存的用户信息再刷新。
    const stored = localStorage.getItem('campus_wall_user')
    if (stored) {
      const user = JSON.parse(stored)
      user.nickname = response.data.nickname
      localStorage.setItem('campus_wall_user', JSON.stringify(user))
    }
    nextNicknameChangeAt.value = response.data.nextNicknameChangeAt || ''
    window.location.reload()
  } catch (e) {
    profileMessage.value = (e.response && e.response.data && e.response.data.message)
      ? '保存失败：' + e.response.data.message
      : '保存失败，请稍后再试。'
  }
}

async function savePassword() {
  if (newPassword.value.length < 6 || newPassword.value.length > 64) {
    passwordMessage.value = '新密码要 6 到 64 个字符'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    passwordMessage.value = '两次输入的新密码不一样'
    return
  }
  try {
    await changePassword({ currentPassword: currentPassword.value, newPassword: newPassword.value })
    // 改密码后旧令牌立刻失效，本地令牌留着也没用，直接清掉回登录页，用新密码重新登录。
    localStorage.removeItem('campus_wall_token')
    localStorage.removeItem('campus_wall_user')
    alert('密码已更改，请用新密码重新登录')
    router.push('/login')
    window.location.reload()
  } catch (e) {
    passwordMessage.value = (e.response && e.response.data && e.response.data.message)
      ? '更改失败：' + e.response.data.message
      : '更改失败，请稍后再试。'
  }
}

function goDetail(id) {
  router.push('/post/' + id)
}

async function edit(post) {
  const title = prompt('新标题', post.title)
  if (title === null) return
  const content = prompt('新内容', post.content)
  if (content === null) return
  await editPost(post.id, { title, content })
  await load()
}

async function remove(id) {
  if (!confirm('确定删除这条帖子吗？')) return
  await deleteOwnPost(id)
  await load()
}

async function deactivate() {
  if (!confirm('确定注销账号吗？注销后学号会被清除，帖子和评论会改为匿名展示，不可恢复。')) return
  // 注销失败时不要静默留在原地：先告诉用户失败了，也不要清本地令牌、不要跳登录页，否则会误以为已经注销了。
  try {
    await deleteAccount()
  } catch (e) {
    alert('注销失败，请稍后再试。如果一直失败，把浏览器里这次请求的报错发给宝锋看。')
    return
  }
  localStorage.removeItem('campus_wall_token')
  localStorage.removeItem('campus_wall_user')
  router.push('/login')
  window.location.reload()
}

onMounted(load)
</script>
