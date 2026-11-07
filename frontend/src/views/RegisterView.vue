<!-- 校园墙项目：注册页：学号只能填 11 位数字，昵称、密码、校区必填，还要选一个密保问题并填答案，忘密码时靠它找回。 -->
<template>
  <div class="card">
    <h2>注册</h2>
    <label>学号</label>
    <input v-model="form.studentId" maxlength="11" placeholder="详见校园一卡通" />
    <label>昵称</label>
    <input v-model="form.nickname" placeholder="非匿名发帖时显示" />
    <label>校区</label>
    <select v-model="form.campusId">
      <option v-for="campus in campuses" :key="campus.id" :value="campus.id">{{ campus.name }}</option>
    </select>
    <label>密码</label>
    <input type="password" v-model="form.password" placeholder="至少 6 位，必须包含字母和数字"/>
    <label>密保问题</label>
    <select v-model="form.securityQuestion">
      <option v-for="question in securityQuestions" :key="question" :value="question">{{ question }}</option>
    </select>
    <label>密保答案</label>
    <input v-model="form.securityAnswer"  />
    <button @click="submit">注册</button>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCampuses, listSecurityQuestions, register } from '../api/posts.js'

const router = useRouter()
const campuses = ref([])
const securityQuestions = ref([])
const message = ref('')
const form = reactive({ studentId: '', nickname: '', password: '', campusId: '', securityQuestion: '', securityAnswer: '' })

async function submit() {
  // 学号先在前端拦一下，11 位数字才发请求，后端同样会再拦一次。
  if (!/^\d{11}$/.test(form.studentId)) {
    message.value = '注册失败，学号只能填 11 位数字'
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,64}$/.test(form.password)) {
    message.value = '注册失败，密码格式：至少 6 位，必须包含字母和数字'
    return
  }
  if (!form.securityQuestion || !form.securityAnswer.trim()) {
    message.value = '注册失败，密保问题和答案都要填写'
    return
  }
  try {
    const response = await register(form)
    localStorage.setItem('campus_wall_token', response.data.token)
    localStorage.setItem('campus_wall_user', JSON.stringify(response.data))
    router.push('/')
    window.location.reload()
  } catch (e) {
    message.value = (e.response && e.response.data && e.response.data.message)
      ? '注册失败：' + e.response.data.message
      : '注册失败，学号可能已被注册，忘记密码请使用密保找回'
  }
}

// 页面加载时先拉校区列表和密保问题列表，注册时让用户选。
onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  if (campuses.value.length) form.campusId = campuses.value[0].id
  try {
    const questions = await listSecurityQuestions()
    securityQuestions.value = questions.data.questions || []
    if (securityQuestions.value.length) form.securityQuestion = securityQuestions.value[0]
  } catch (e) {
    securityQuestions.value = []
  }
})
</script>
