<!-- 校园墙项目：发帖页：选择板块后动态显示专属字段，失物填地点和时间，二手填价格和成色，可以勾选匿名和上传图片。 -->
<!-- 联系方式不在这里填了，统一在“我的资料”里填好永久保存；发帖时只选要不要公开它。发布前要先勾“我确认无违规内容”。 -->
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
      <option value="VENT">喝茶闲聊</option>
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
    <label>图片</label>
    <input type="file" accept="image/*" multiple @change="onFiles" />
    <label><input type="checkbox" v-model="form.anonymous" style="width:auto;" /> 匿名发布</label>
    <label><input type="checkbox" v-model="form.showContactInfo" style="width:auto;" /> 公开我的联系方式</label>
    <p v-if="hasContactInfo"></p>
    <p v-else>你还没有在“我的资料”里填联系方式，勾了公开也不会显示，先去“我的资料”里填一个吧~</p>
    <label><input type="checkbox" v-model="confirmed" style="width:auto;" /> 我确认无违规内容</label>
    <button @click="submit">发布</button>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPost, getProfile, listCampuses, uploadImage } from '../api/posts.js'

const router = useRouter()
const campuses = ref([])
const files = ref([])
const message = ref('')
const confirmed = ref(false)
const hasContactInfo = ref(false)
const form = reactive({
  campusId: '',
  category: 'LOST',
  title: '',
  content: '',
  anonymous: false,
  showContactInfo: false,
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
  // 发布前必须先勾确认框，没勾就不发，免得手滑把没检查过的内容发出去了。
  if (!confirmed.value) {
    message.value = '请先勾选“我确认无违规内容”再发布'
    return
  }
  try {
    const imageUrls = []
    for (const file of files.value) {
      const response = await uploadImage(file)
      imageUrls.push(response.data.url)
    }
    const payload = { ...form, imageUrls }
    if (payload.happenedAt) payload.happenedAt = payload.happenedAt + ':00'
    await createPost(payload)
    router.push('/')
  } catch (e) {
    message.value = (e.response && e.response.data && e.response.data.message)
      ? '发布失败：' + e.response.data.message
      : (e.response ? '发布失败（' + e.response.status + '），请稍后再试' : '发布失败，请稍后再试')
  }
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  if (campuses.value.length) form.campusId = campuses.value[0].id
  try {
    const profile = (await getProfile()).data
    hasContactInfo.value = !!(profile.contactInfo && profile.contactInfo.trim())
  } catch (e) {
    hasContactInfo.value = false
  }
})
</script>
