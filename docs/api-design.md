# 接口设计

基础路径是 /api，登录后需要在请求头带上 Authorization: Bearer 加 JWT。

## 公开接口

- GET /api/campuses：校区列表
- GET /api/posts?campusId=&category=&keyword=&page=&size=：帖子列表
- GET /api/posts/{id}：帖子详情，包含图片、专属字段、评论
- POST /api/auth/register：学号、昵称、密码、校区注册
- POST /api/auth/login：学号、密码登录，返回 JWT 和用户信息

## 登录后接口

- POST /api/posts：发帖，包含板块专属字段和图片地址
- DELETE /api/posts/{id}：作者自己删帖，改为 DELETED
- POST /api/posts/{id}/comments：评论
- POST /api/posts/{id}/like：点赞，再次调用取消点赞
- POST /api/posts/{id}/favorite：收藏，再次调用取消收藏
- POST /api/posts/{id}/report：举报，带原因
- POST /api/uploads：上传图片，返回图片地址，只接受图片类型，单张不超过 5MB

## 管理员接口

只有 role 是 ADMIN 的账号可以调用：

- GET /api/admin/posts：管理端帖子列表
- DELETE /api/admin/posts/{id}：管理员删帖
- GET /api/admin/users：管理端用户列表
- POST /api/admin/users/{id}/ban：封禁账号
- POST /api/admin/users/{id}/unban：解封账号
- GET /api/admin/reports：举报列表
- POST /api/admin/reports/{id}/handle：标记举报已处理

## 显示规则

- 帖子 anonymous 是 true 时，前端和接口都只返回匿名用户，不返回真实昵称和学号。
- 帖子 anonymous 是 false 时，显示注册昵称，不显示学号。
- 任何接口都不返回 password_hash。
