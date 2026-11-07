<!-- 校园墙项目：我的资料页：改昵称、改密码、改密保问题和注销账号都在这里，和“我的帖子”平行，不再放在帖子列表下面。 -->
<template>
  <div>
    <h2>我的资料</h2>
    <div class="card">
      <h3>昵称</h3>
      <label>昵称</label>
      <input v-model="nickname" maxlength="64" />
      <p v-if="nextNicknameChangeAt">下次可改昵称：{{ nextNicknameChangeAt }}</p>
      <p v-else>现在可以改昵称。更改后，30天内无法更改</p>
      <button @click="saveProfile">保存资料</button>
      <p>{{ profileMessage }}</p>
    </div>
    <div class="card">
      <h3>联系方式</h3>
      <p>联系方式填一次就永久保存，发帖时你自己选要不要公开它，不公开别人就看不到。</p>
      <label>联系方式</label>
      <input v-model="contactInfo" maxlength="128" placeholder="例如：微信号、手机号、QQ 号" />
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
      <p>密码格式：至少 6 位，必须包含字母和数字</p>
      <label>确认新密码</label>
      <input type="password" v-model="confirmPassword" />
      <button @click="savePassword">更改密码</button>
      <p>{{ passwordMessage }}</p>
    </div>
    <div class="card">
      <h3>密保问题</h3>
      <p v-if="hasSecurityQuestion">你已经设置过密保问题了，忘密码时可以用它重置；想换问题和答案的话，在下面重新设置，换的时候也要先输对原密码。</p>
      <p v-else>你还没有设置密保问题，忘密码时用不了“忘记密码”入口，先在这里设置一个，之后才能用它找回密码。</p>
      <label>密保问题</label>
      <select v-model="securityQuestion">
        <option v-for="question in securityQuestions" :key="question" :value="question">{{ question }}</option>
      </select>
      <label>密保答案</label>
      <input v-model="securityAnswer" placeholder="忘密码时要回答这个问题，答案要自己记得住" />
      <label>原密码</label>
      <input type="password" v-model="securityCurrentPassword" placeholder="设置或更换密保要先输对原密码" />
      <button @click="saveSecurityQuestion">保存密保问题</button>
      <p>{{ securityMessage }}</p>
    </div>
    <div class="card">
      <h3>注销账号</h3>
      <p>注销后你的学号会改成原学号加 _delete，密码会被清空，你的帖子和评论会改为匿名展示，这个操作不可恢复。</p>
      <button @click="deactivate">注销我的账号</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, deleteAccount, getProfile, listSecurityQuestions, setSecurityQuestion, updateProfile } from '../api/posts.js'

const router = useRouter()
const nickname = ref('')
const contactInfo = ref('')
const nextNicknameChangeAt = ref('')
const profileMessage = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordMessage = ref('')
const securityQuestions = ref([])
const securityQuestion = ref('')
const securityAnswer = ref('')
const securityCurrentPassword = ref('')
const securityMessage = ref('')
const hasSecurityQuestion = ref(false)

async function load() {
  try {
    // 资料接口不返回学号，所以这里只有昵称和下次可改日期，学号根本不显示、也不让改。
    // 如果已经设过密保，后端会把当前选的问题也返回回来，下拉框就默认选中它，而不是每次都回到第一个问题。
    const profile = (await getProfile()).data
    nickname.value = profile.nickname || ''
    contactInfo.value = profile.contactInfo || ''
    nextNicknameChangeAt.value = profile.nextNicknameChangeAt || ''
    hasSecurityQuestion.value = !!profile.hasSecurityQuestion
    if (profile.securityQuestion) securityQuestion.value = profile.securityQuestion
  } catch (e) {
  }
  try {
    // 密保问题列表从后端拿，和注册页用的是同一份预设列表，前端不另外硬编码。
    const questions = (await listSecurityQuestions()).data
    securityQuestions.value = questions.questions || []
    if (!securityQuestion.value && securityQuestions.value.length) securityQuestion.value = securityQuestions.value[0]
  } catch (e) {
    securityQuestions.value = []
  }
}

async function saveProfile() {
  try {
    const response = await updateProfile({ nickname: nickname.value, contactInfo: contactInfo.value })
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
  if (!/^(?=.*[A-Za-z])(?=.*\d).{6,64}$/.test(newPassword.value)) {
    passwordMessage.value = '新密码格式：至少 6 位，必须包含字母和数字'
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

async function saveSecurityQuestion() {
  if (!securityQuestion.value || !securityAnswer.value.trim()) {
    securityMessage.value = '密保问题和答案都要填'
    return
  }
  try {
    // 只改密保不改密码，令牌不受影响，保存成功不用重新登录；答案框和原密码框清空，避免答案留在页面上。
    await setSecurityQuestion({ currentPassword: securityCurrentPassword.value, securityQuestion: securityQuestion.value, securityAnswer: securityAnswer.value })
    hasSecurityQuestion.value = true
    securityAnswer.value = ''
    securityCurrentPassword.value = ''
    securityMessage.value = '密保问题已保存，忘密码时可以用它重置'
  } catch (e) {
    securityMessage.value = (e.response && e.response.data && e.response.data.message)
      ? '保存失败：' + e.response.data.message
      : '保存失败，请稍后再试。'
  }
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
