<!-- 校园墙项目：帖子详情页：展示正文、图片和专属字段，可以点赞、收藏、举报和发表评论。 -->
<template>
  <div v-if="post">
    <div class="card">
      <h2>{{ post.title }}</h2>
      <p>{{ post.displayName }} · {{ post.campusName }} · {{ post.createdAt }}</p>
      <p>{{ post.content }}</p>
      <div v-if="post.price">价格：¥{{ post.price }}，成色：{{ post.conditionText }}</div>
      <div v-if="post.location">地点：{{ post.location }}，时间：{{ post.happenedAt }}</div>
      <div v-if="post.contactInfo">联系方式：{{ post.contactInfo }}</div>
      <div v-if="post.images">
        <img v-for="url in post.images" :key="url" :src="url" alt="" style="width:160px;height:160px;object-fit:cover;margin-right:8px;" />
      </div>
      <div>
        <button @click="like">点赞 {{ post.likeCount }}</button>
        <button @click="favorite">收藏 {{ post.favoriteCount }}</button>
        <button @click="report">举报</button>
      </div>
    </div>
    <div class="card">
      <h3>评论</h3>
      <div v-for="comment in post.comments" :key="comment.id">
        <p><strong>{{ comment.displayName }}</strong>：{{ comment.content }}</p>
      </div>
      <textarea v-model="commentText" placeholder="写下你的评论"></textarea>
      <label><input type="checkbox" v-model="commentAnonymous" style="width:auto;" /> 匿名评论</label>
      <button @click="sendComment">发送评论</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { addComment, getPost, reportPost, toggleFavorite, toggleLike } from '../api/posts.js'

const route = useRoute()
const post = ref(null)
const commentText = ref('')
const commentAnonymous = ref(false)

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

async function sendComment() {
  if (!commentText.value.trim()) return
  await addComment(route.params.id, { content: commentText.value, anonymous: commentAnonymous.value })
  commentText.value = ''
  await load()
}

onMounted(load)
</script>
