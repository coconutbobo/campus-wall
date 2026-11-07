<!-- 校园墙项目：忘密码页：先输学号查出注册时选的密保问题，再答密保答案、设新密码，不用登录就能重置。 -->
<template>
  <div class="card">
    <h2>忘记密码</h2>
    <p>输入学号，先查出你注册时选的密保问题，答对了才能设新密码。</p>
    <label>学号</label>
    <input v-model="studentId" maxlength="11" placeholder="注册时填的 11 位数字学号" />
    <button @click="loadQuestion" v-if="!question">下一步</button>
    <div v-if="question">
      <label>密保问题</label>
      <p>{{ question }}</p>
      <label>密保答案</label>
      <input v-model="securityAnswer" />
      <label>新密码</label>
      <input type="password" v-model="newPassword" />
      <p>密码格式：至少 6 位，必须包含字母和数字</p>
      <label>确认新密码</label>
      <input type="password" v-model="confirmPassword" />
      <button @click="submit">重置密码</button>
    </div>
    <p>想起密码了？<router-link to="/login">回登录页</router-link></p>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPassword, getSecurityQuestion } from '../api/posts.js'

const router = useRouter()
const studentId = ref('')
const question = ref('')
const securityAnswer = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const message = ref('')

async function loadQuestion() {
  if (!/^\d{11}$/.test(studentId.value)) {
    message.value = '学号只能填 11 位数字'
    return
  }
  try {
    const response = await getSecurityQuestion(studentId.value)
    question.value = response.data.securityQuestion
    message.value = ''
  } catch (e) {
    message.value = (e.response && e.response.data && e.response.data.message)
      ? '查不到密保问题：' + e.response.data.message
      : '查不到密保问题，请检查学号'
  }
}

async function submit() {
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,64}$/.test(newPassword.value)) {
    message.value = '新密码格式：至少 6 位，必须包含字母和数字'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    message.value = '两次输入的新密码不一样'
    return
  }
  try {
    await forgotPassword({ studentId: studentId.value, securityAnswer: securityAnswer.value, newPassword: newPassword.value })
    message.value = '密码已重置，请用新密码登录'
    setTimeout(() => router.push('/login'), 1000)
  } catch (e) {
    message.value = (e.response && e.response.data && e.response.data.message)
      ? '重置失败：' + e.response.data.message
      : '重置失败，请稍后再试'
  }
}
</script>
