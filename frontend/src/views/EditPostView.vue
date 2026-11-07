<!-- 校园墙项目：编辑帖子页：和发帖用同一套表单，打开时先把帖子原内容填进去，改完保存回详情页。 -->
<!-- 校区和板块发完就不能改了，这里只展示不让选；标题、内容、各板块专属字段、匿名、联系方式公开和图片都能改。 -->
<template>
  <div class="card" v-if="loaded">
    <h2>编辑帖子</h2>
    <label>校区</label>
    <select v-model="form.campusId" disabled>
      <option v-for="campus in campuses" :key="campus.id" :value="campus.id">{{ campus.name }}</option>
    </select>
    <label>板块</label>
    <select v-model="form.category" disabled>
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
    <div v-if="keptImages.length">
      <span v-for="url in keptImages" :key="url" style="display:inline-block;position:relative;margin-right:8px;">
        <img :src="url" alt="" style="width:120px;height:120px;object-fit:cover;border-radius:8px;" />
        <button type="button" @click="keptImages = keptImages.filter(u => u !== url)" style="position:absolute;top:2px;right:2px;">✕</button>
      </span>
    </div>
    <input type="file" accept="image/*" multiple @change="onFiles" />
    <label><input type="checkbox" v-model="form.anonymous" style="width:auto;" /> 匿名发布</label>
    <label><input type="checkbox" v-model="form.showContactInfo" style="width:auto;" /> 公开我的联系方式</label>
    <p v-if="hasContactInfo">联系方式是“我的资料”里填的那一个，勾了别人才看得到，不勾就不显示。</p>
    <p v-else>你还没有在“我的资料”里填联系方式，勾了公开也不会显示，先去“我的资料”里填一个。</p>
    <label><input type="checkbox" v-model="confirmed" style="width:auto;" /> 我确认无违规内容</label>
    <button @click="submit">保存修改</button>
    <button @click="router.push('/post/' + route.params.id)">取消</button>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { editPost, getPost, getProfile, listCampuses, uploadImage } from '../api/posts.js'

const route = useRoute()
const router = useRouter()
const campuses = ref([])
const files = ref([])
const message = ref('')
const confirmed = ref(false)
const hasContactInfo = ref(false)
const loaded = ref(false)
// 原来已经传过的图片，点 ✕ 就从保留列表里去掉，保存时只把留下的和新传的一起发过去。
const keptImages = ref([])
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
  // 和发帖一样，保存前必须先勾确认框。
  if (!confirmed.value) {
    message.value = '请先勾选“我确认无违规内容”再保存'
    return
  }
  try {
    const imageUrls = [...keptImages.value]
    for (const file of files.value) {
      const response = await uploadImage(file)
      imageUrls.push(response.data.url)
    }
    const payload = { ...form, imageUrls }
    if (payload.happenedAt) payload.happenedAt = payload.happenedAt + ':00'
    await editPost(route.params.id, payload)
    router.push('/post/' + route.params.id)
  } catch (e) {
    message.value = (e.response && e.response.data && e.response.data.message)
      ? '保存失败：' + e.response.data.message
      : (e.response ? '保存失败（' + e.response.status + '），请稍后再试' : '保存失败，请稍后再试')
  }
}

onMounted(async () => {
  const response = await listCampuses()
  campuses.value = response.data
  const post = (await getPost(route.params.id)).data
  form.campusId = post.campusId
  form.category = post.category
  form.title = post.title
  form.content = post.content
  form.anonymous = !!post.anonymous
  form.showContactInfo = !!post.showContactInfo
  form.lostKind = post.lostKind || 'LOST'
  form.location = post.location || ''
  // 后端返回的时间带秒，datetime-local 输入框只要到分钟，截掉最后的秒。
  form.happenedAt = post.happenedAt ? String(post.happenedAt).slice(0, 16) : ''
  form.price = post.price ?? null
  form.conditionText = post.conditionText || ''
  form.mood = post.mood || ''
  keptImages.value = post.images || []
  try {
    const profile = (await getProfile()).data
    hasContactInfo.value = !!(profile.contactInfo && profile.contactInfo.trim())
  } catch (e) {
    hasContactInfo.value = false
  }
  loaded.value = true
})
</script>
