// 校园墙项目
// 接口函数集合：校区、帖子、评论、点赞、收藏、举报、上传和管理后台的请求都集中在这里，页面只管调用。
// 注意：所有接口返回的都是公开 ID，绝不返回学号；登录和注册返回的也是公开 ID。
import client from './client.js'

export function listCampuses() {
  return client.get('/campuses')
}

export function listPosts(params) {
  return client.get('/posts', { params })
}

export function getPost(id) {
  return client.get('/posts/' + id)
}

export function createPost(payload) {
  return client.post('/posts', payload)
}

// 作者编辑自己的帖子，匿名帖子同样靠登录身份判断，本人不会因为匿名就失去编辑权。
export function editPost(id, payload) {
  return client.patch('/posts/' + id, payload)
}

export function deleteOwnPost(id) {
  return client.delete('/posts/' + id)
}

// 本人帖子列表：包括匿名帖子，本人能从这里管理自己的匿名帖子。
export function myPosts() {
  return client.get('/me/posts')
}

// 校区订阅：订阅后首页可以选择只看订阅校区的内容。
export function listSubscriptions() {
  return client.get('/me/subscriptions')
}

export function subscribeCampus(campusId) {
  return client.post('/me/subscriptions', { campusId })
}

export function unsubscribeCampus(campusId) {
  return client.delete('/me/subscriptions/' + campusId)
}

export function addComment(id, payload) {
  return client.post('/posts/' + id + '/comments', payload)
}

export function toggleLike(id) {
  return client.post('/posts/' + id + '/like')
}

export function toggleFavorite(id) {
  return client.post('/posts/' + id + '/favorite')
}

export function reportPost(id, reason) {
  return client.post('/posts/' + id + '/report', { reason })
}

// 举报评论：举报时要写原因，举报人信息不会公开显示。
export function reportComment(commentId, reason) {
  return client.post('/posts/comments/' + commentId + '/report', { reason })
}

export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/uploads', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function login(payload) {
  return client.post('/auth/login', payload)
}

// 注销登录：服务端会让旧令牌失效，前端也要清除本地令牌。
export function logout() {
  return client.post('/auth/logout')
}

export function register(payload) {
  return client.post('/auth/register', payload)
}

// 注销当前账号：学号会被清除，自己的帖子和评论会改为匿名展示。
export function deleteAccount() {
  return client.delete('/auth/me')
}

export function adminPosts() {
  return client.get('/admin/posts')
}

export function adminUsers() {
  return client.get('/admin/users')
}

export function adminReports() {
  return client.get('/admin/reports')
}

export function adminAuditLogs() {
  return client.get('/admin/audit-logs')
}

export function adminDeletePost(id, reason) {
  return client.delete('/admin/posts/' + id, { data: { reason } })
}

export function adminDeleteComment(id, reason) {
  return client.delete('/admin/comments/' + id, { data: { reason } })
}

// 封禁账号：type 是 TEMPORARY 临时或 PERMANENT 永久，临时要传天数，理由会记进审计日志。
export function adminBanUser(publicId, payload) {
  return client.post('/admin/users/' + publicId + '/ban', payload)
}

export function adminUnbanUser(publicId, reason) {
  return client.post('/admin/users/' + publicId + '/unban', { reason })
}

export function adminHandleReport(id, reason) {
  return client.post('/admin/reports/' + id + '/handle', { reason })
}

// 管理员查看单个用户的学号：必须传理由，这次查看会被记进审计日志，普通用户列表里看不到学号。
export function adminViewIdentity(publicId, reason) {
  return client.get('/admin/users/' + publicId + '/identity', { params: { reason } })
}
