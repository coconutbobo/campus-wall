// 校园墙项目
// 接口函数集合：校区、帖子、评论、点赞、收藏、举报、上传和管理后台的请求都集中在这里，页面只管调用。
import client from './client.js'

// 函数：listCampuses，具体逻辑见函数体。
export function listCampuses() {
  return client.get('/campuses')
}

// 函数：listPosts，具体逻辑见函数体。
export function listPosts(params) {
  return client.get('/posts', { params })
}

// 函数：getPost，具体逻辑见函数体。
export function getPost(id) {
  return client.get('/posts/' + id)
}

// 函数：createPost，具体逻辑见函数体。
export function createPost(payload) {
  return client.post('/posts', payload)
}

// 发表评论：评论也可以选择匿名，匿名评论同样只显示匿名用户。
export function addComment(id, payload) {
  return client.post('/posts/' + id + '/comments', payload)
}

// 点赞切换：已经点过赞就取消，没点过就新增，同一个用户对同一个帖子只能有一条点赞记录。
export function toggleLike(id) {
  return client.post('/posts/' + id + '/like')
}

// 收藏切换：逻辑和点赞一样，已经收藏就取消，没收藏就新增。
export function toggleFavorite(id) {
  return client.post('/posts/' + id + '/favorite')
}

// 函数：reportPost，具体逻辑见函数体。
export function reportPost(id, reason) {
  return client.post('/posts/' + id + '/report', { reason })
}

// 函数：uploadImage，具体逻辑见函数体。
export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/uploads', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// 登录：核对学号和密码，封禁账号直接拒绝，成功后返回令牌。
export function login(payload) {
  return client.post('/auth/login', payload)
}

// 注册新账号：先检查学号有没有被注册过，再把密码加密后保存。
export function register(payload) {
  return client.post('/auth/register', payload)
}

// 函数：adminPosts，具体逻辑见函数体。
export function adminPosts() {
  return client.get('/admin/posts')
}

// 函数：adminUsers，具体逻辑见函数体。
export function adminUsers() {
  return client.get('/admin/users')
}

// 函数：adminReports，具体逻辑见函数体。
export function adminReports() {
  return client.get('/admin/reports')
}

// 函数：adminDeletePost，具体逻辑见函数体。
export function adminDeletePost(id) {
  return client.delete('/admin/posts/' + id)
}

// 函数：adminBanUser，具体逻辑见函数体。
export function adminBanUser(id) {
  return client.post('/admin/users/' + id + '/ban')
}

// 函数：adminUnbanUser，具体逻辑见函数体。
export function adminUnbanUser(id) {
  return client.post('/admin/users/' + id + '/unban')
}

// 函数：adminHandleReport，具体逻辑见函数体。
export function adminHandleReport(id) {
  return client.post('/admin/reports/' + id + '/handle')
}
