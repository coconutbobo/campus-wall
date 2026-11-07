<!-- 校园墙项目：帖子详情页：展示正文、图片和专属字段，可以点赞、收藏、举报和发表评论。 -->
<template>
  <div v-if="post">
    <div class="card">
      <h2>{{ post.title }}</h2>
      <p>{{ post.displayName }} · {{ post.campusName }} · {{ post.createdAt }}</p>
      <p v-if="isAdmin && post.authorNickname">作者昵称（仅管理员可见）：{{ post.authorNickname }}</p>
      <p>{{ post.content }}</p>
      <div v-if="post.price">价格：¥{{ post.price }}，成色：{{ post.conditionText }}</div>
      <div v-if="post.location">地点：{{ post.location }}，时间：{{ post.happenedAt }}</div>
      <div v-if="post.contactInfo">联系方式：{{ post.contactInfo }}</div>
      <div v-if="post.images">
        <img v-for="url in post.images" :key="url" :src="url" alt="" style="width:160px;height:160px;object-fit:cover;margin-right:8px;cursor:zoom-in;" @click="viewerUrl = url" />
      </div>
      <ImageViewer v-if="viewerUrl" :src="viewerUrl" @close="viewerUrl = null" />
      <div>
        <button @click="like">点赞 {{ post.likeCount }}</button>
        <button @click="favorite">收藏 {{ post.favoriteCount }}</button>
        <button @click="report">举报</button>
        <button v-if="post.isMine" @click="router.push('/post/' + post.id + '/edit')">编辑我的帖子</button>
      </div>
      <p v-if="post.isMine">这是你自己的帖子，匿名也不会让你失去编辑和删除的权利。</p>
    </div>
    <div class="card">
      <h3>评论</h3>
      <div v-for="comment in post.comments" :key="comment.id">
        <p><strong>{{ comment.displayName }}</strong>：{{ comment.content }} <button @click="reportComment(comment.id)">举报评论</button></p>
      </div>
      <textarea v-model="commentText" placeholder="写下你的评论"></textarea>
      <label><input type="checkbox" v-model="commentAnonymous" style="width:auto;" /> 匿名评论</label>
      <button @click="sendComment">发送评论</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addComment, getPost, reportComment as reportCommentApi, reportPost, toggleFavorite, toggleLike } from '../api/posts.js'
import ImageViewer from '../components/ImageViewer.vue'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const commentText = ref('')
const commentAnonymous = ref(false)

// 正在查看的完整图片地址，有值就弹出查看器，关掉就清空。
const viewerUrl = ref(null)

// 管理员才能在详情里看到匿名作者的真实昵称，普通用户看不到。
const isAdmin = computed(() => {
  try {
    const stored = JSON.parse(localStorage.getItem('campus_wall_user') || 'null')
    return stored && stored.role === 'ADMIN'
  } catch (e) {
    return false
  }
})

async function load() {
  const response = await getPost(route.params.id)
  post.value = response.data
}

async function like() {
  await toggleLike(route.params.id)
  await load()
}

async function favorite() {
  await toggleFavorite(route.params.id)
  await load()
}

async function report() {
  const reason = prompt('请填写举报原因')
  if (reason) {
    await reportPost(route.params.id, reason)
    alert('已提交举报')
  }
}

async function reportComment(commentId) {
  const reason = prompt('请填写举报评论的原因')
  if (reason) {
    await reportCommentApi(commentId, reason)
    alert('已提交举报')
  }
}

async function sendComment() {
  if (!commentText.value.trim()) return
  await addComment(route.params.id, { content: commentText.value, anonymous: commentAnonymous.value })
  commentText.value = ''
  await load()
}

onMounted(load)
</script>
