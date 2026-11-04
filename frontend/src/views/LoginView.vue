<!-- 校园墙项目：登录页：用学号和密码登录，成功后把令牌和用户信息存到本地，之后发帖就不用重复登录。 -->
<template>
  <div class="card">
    <h2>登录</h2>
    <label>学号</label>
    <input v-model="studentId" />
    <label>密码</label>
    <input type="password" v-model="password" />
    <button @click="submit">登录</button>
    <p>还没有账号？<router-link to="/register">去注册</router-link></p>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/posts.js'

const router = useRouter()
const studentId = ref('')
const password = ref('')
const message = ref('')

async function submit() {
  try {
    const response = await login({ studentId: studentId.value, password: password.value })
    localStorage.setItem('campus_wall_token', response.data.token)
    localStorage.setItem('campus_wall_user', JSON.stringify(response.data))
    router.push('/')
    window.location.reload()
  } catch (e) {
    message.value = '登录失败，请检查学号和密码'
  }
}
</script>
