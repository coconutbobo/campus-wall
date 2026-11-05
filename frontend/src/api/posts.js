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

// 本人收藏列表：只返回当前用户收藏过的、还没被删除的帖子。
export function myFavorites() {
  return client.get('/me/favorites')
}

// 公告：所有人都能看，只有管理员能发和删。
export function listAnnouncements() {
  return client.get('/announcements')
}

export function createAnnouncement(payload) {
  return client.post('/announcements', payload)
}

export function deleteAnnouncement(id) {
  return client.delete('/announcements/' + id)
}

// 我的资料：只返回公开 ID、昵称、角色和校区名，不返回学号。
export function getProfile() {
  return client.get('/me/profile')
}

// 改资料：只改昵称，学号不给用户改；昵称 30 天只能改一次，冷却期内后端会拒绝并告诉下次可改日期。
export function updateProfile(payload) {
  return client.patch('/me/profile', payload)
}

// 改密码：必须登录，先输原密码，改完旧令牌失效，要重新登录。
export function changePassword(payload) {
  return client.patch('/me/password', payload)
}

// 补设 / 更换密保问题：给注册时还没有密保的老账号用的，必须登录，先输原密码；只改密保不改密码，令牌不受影响。
export function setSecurityQuestion(payload) {
  return client.patch('/me/security-question', payload)
}

// 密保问题：注册页从后端拿预设问题列表，忘密码时按学号查用户注册时选的问题。
export function listSecurityQuestions() {
  return client.get('/auth/security-questions')
}

export function getSecurityQuestion(studentId) {
  return client.get('/auth/security-question', { params: { studentId } })
}

// 忘密码重置：不用登录，传学号、密保答案和新密码，答案对了才能改。
export function forgotPassword(payload) {
  return client.post('/auth/forgot-password', payload)
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
