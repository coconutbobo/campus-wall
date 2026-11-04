<!-- 校园墙项目：注册页：填写学号、昵称、密码和校区，非匿名发帖时显示的就是这里填的昵称。 -->
<template>
  <div class="card">
    <h2>注册</h2>
    <label>学号</label>
    <input v-model="form.studentId" />
    <label>昵称</label>
    <input v-model="form.nickname" placeholder="非匿名发帖时显示这个昵称" />
    <label>校区</label>
    <select v-model="form.campusId">
      <option v-for="campus in campuses" :key="campus.id" :value="campus.id">{{ campus.name }}</option>
    </select>
    <label>密码</label>
    <input type="password" v-model="form.password" />
    <button @click="submit">注册</button>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCampuses, register } from '../api/posts.js'

const router = useRouter()
const campuses = ref([])
const message = ref('')
const form = reactive({ studentId: '', nickname: '', password: '', campusId: '' })

async function submit() {
  try {
    const response = await register(form)
    localStorage.setItem('campus_wall_token', response.data.token)
    localStorage.setItem('campus_wall_user', JSON.stringify(response.data))
    router.push('/')
    window.location.reload()
  } catch (e) {
    message.value = '注册失败，学号可能已被注册'
  }
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  if (campuses.value.length) form.campusId = campuses.value[0].id
})
</script>
