<!-- 校园墙项目：发帖页：选择板块后动态显示专属字段，失物填地点和时间，二手填价格和成色，可以勾选匿名和上传图片。 -->
<template>
  <div class="card">
    <h2>发帖</h2>
    <label>校区</label>
    <select v-model="form.campusId">
      <option v-for="campus in campuses" :key="campus.id" :value="campus.id">{{ campus.name }}</option>
    </select>
    <label>板块</label>
    <select v-model="form.category">
      <option value="LOST">失物招领</option>
      <option value="SECONDHAND">二手交易</option>
      <option value="QA">提问求助</option>
      <option value="VENT">吐槽</option>
    </select>
    <label>标题</label>
    <input v-model="form.title" />
    <label>内容</label>
    <textarea v-model="form.content" rows="5"></textarea>
    <div v-if="form.category === 'LOST'">
      <label>丢了还是捡到</label>
      <select v-model="form.lostKind">
        <option value="LOST">丢了东西</option>
        <option value="FOUND">捡到东西</option>
      </select>
      <label>地点</label>
      <input v-model="form.location" />
      <label>发生时间</label>
      <input type="datetime-local" v-model="form.happenedAt" />
    </div>
    <div v-if="form.category === 'SECONDHAND'">
      <label>价格</label>
      <input type="number" v-model="form.price" />
      <label>成色</label>
      <input v-model="form.conditionText" placeholder="例如：九成新" />
    </div>
    <div v-if="form.category === 'VENT'">
      <label>心情标签</label>
      <input v-model="form.mood" />
    </div>
    <label>联系方式</label>
    <input v-model="form.contactInfo" placeholder="二手和失物建议填写，线下自行交易" />
    <label>图片</label>
    <input type="file" accept="image/*" multiple @change="onFiles" />
    <label><input type="checkbox" v-model="form.anonymous" style="width:auto;" /> 匿名发布，显示为匿名用户</label>
    <p>匿名只是展示方式：其他人只看到匿名用户，不会看到你的昵称和公开 ID，但管理员在处理违规时仍能追责；正文里不要写手机号、身份证号，系统会直接拦截这类内容。</p>
    <button @click="submit">发布</button>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPost, listCampuses, uploadImage } from '../api/posts.js'

const router = useRouter()
const campuses = ref([])
const files = ref([])
const message = ref('')
const form = reactive({
  campusId: '',
  category: 'LOST',
  title: '',
  content: '',
  anonymous: false,
  contactInfo: '',
  lostKind: 'LOST',
  location: '',
  happenedAt: '',
  price: null,
  conditionText: '',
  mood: ''
})

function onFiles(event) {
  files.value = Array.from(event.target.files)
}

async function submit() {
  message.value = ''
  const imageUrls = []
  for (const file of files.value) {
    const response = await uploadImage(file)
    imageUrls.push(response.data.url)
  }
  const payload = { ...form, imageUrls }
  if (payload.happenedAt) payload.happenedAt = payload.happenedAt + ':00'
  await createPost(payload)
  router.push('/')
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  if (campuses.value.length) form.campusId = campuses.value[0].id
})
</script>
