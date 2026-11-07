# 接口设计

基础路径是 /api，登录后需要在请求头带上 Authorization: Bearer 加 JWT。令牌里只放公开 ID，不放学号，有效期 24 小时，过期后需要重新登录。

## 公开接口

- GET /api/campuses：校区列表，只返回校区主键、名称和编码，不返回敏感信息
- GET /api/posts?campusId=&campusIds=&subscribedOnly=&category=&keyword=&page=&size=：帖子列表，分页返回，每页最多 50 条；subscribedOnly 为 true 时只返回当前用户订阅的校区帖子
- GET /api/posts/{id}：帖子详情，包含图片、专属字段、正常状态的评论
- POST /api/auth/register：学号、昵称、密码、校区、密保问题和密保答案注册，返回公开 ID 和令牌，不返回学号；学号只能是 11 位数字，密保问题必须从预设列表里选，答案必填，密码和密保答案都只存 BCrypt 加密值
- POST /api/auth/login：学号、密码登录，返回公开 ID 和令牌，不返回学号；登录时学号不限 11 位数字，老账号（比如 owner）还能正常登录
- GET /api/auth/security-questions：密保问题预设列表，注册页用
- GET /api/auth/security-question?studentId=：按学号查该账号注册时选的密保问题，只返回问题，不返回答案
- POST /api/auth/forgot-password：忘密码重置，不用登录，传学号、密保答案和新密码，答案对了才能改，改完旧令牌全部失效，要重新登录；没有设置密保问题的老账号可以在登录后到“我的资料”页补设

## 登录后接口

- POST /api/auth/logout：注销登录，令牌版本号递增，旧令牌立刻失效，前端也要清除本地令牌
- DELETE /api/auth/me：注销账号，学号会被清除，自己的帖子和评论会改为匿名展示
- POST /api/posts：发帖，包含板块专属字段和图片地址，每人每小时最多 10 条，同一网络地址哈希每小时最多 50 条
- PATCH /api/posts/{id}：作者编辑自己的帖子，匿名帖子同样靠登录身份判断
- DELETE /api/posts/{id}：作者自己删帖，改为 DELETED
- GET /api/me/posts：当前用户自己的帖子列表，包括匿名帖子，分页返回
- GET /api/me/profile：当前用户自己的资料，只返回公开 ID、昵称、角色、校区名、下次可改昵称的日期、是否已设密保和当前选的密保问题，不返回学号和密保答案；没改过昵称时下次可改日期为空
- PATCH /api/me/profile：只改自己的昵称，传 nickname；学号不给用户改，传了 studentId 也会被后端直接忽略；昵称 30 天只能改一次，传的昵称和现在一样不算改，冷却期内返回 400 和下次可改日期
- PATCH /api/me/password：改自己的密码，必须先输对原密码 currentPassword，新密码 newPassword 至少 6 位，必须包含字母和数字，改完旧令牌全部失效，要重新登录，原密码错了返回 400，不会把人踢下线
- PATCH /api/me/security-question：补设 / 更换自己的密保问题，传原密码 currentPassword、密保问题 securityQuestion 和密保答案 securityAnswer；问题必须从预设列表里选，答案 1 到 64 个字符，只存 BCrypt 加密值；只改密保不改密码，令牌不受影响，不用重新登录
- GET /api/me/subscriptions：当前用户的校区订阅列表
- POST /api/me/subscriptions：订阅一个校区，传 campusId
- DELETE /api/me/subscriptions/{campusId}：取消订阅一个校区
- POST /api/posts/{id}/comments：评论，每人每小时最多 30 条，匿名评论规则和帖子一致
- POST /api/posts/{id}/like：点赞，再次调用取消点赞
- POST /api/posts/{id}/favorite：收藏，再次调用取消收藏
- POST /api/posts/{id}/report：举报帖子，带原因
- POST /api/posts/comments/{commentId}/report：举报评论，带原因
- POST /api/uploads：上传图片，返回图片地址，只接受 JPEG、PNG、GIF、WebP 的真实文件头，拒绝 SVG 和其他类型，单张不超过 5MB

## 管理员接口

只有 role 是 ADMIN 的账号可以调用，安全配置会直接拦截非管理员请求，不是只在前端隐藏按钮：

- GET /api/admin/posts：管理端帖子列表，分页返回
- DELETE /api/admin/posts/{id}：管理员删帖，理由会记进审计日志
- DELETE /api/admin/comments/{id}：管理员删评论，理由会记进审计日志
- GET /api/admin/users：管理端用户列表，分页返回，不返回学号，只返回公开 ID、昵称、角色和封禁状态
- GET /api/admin/users/{publicId}/identity：管理员查看单个用户的学号，必须传理由，这次查看会被记进审计日志
- POST /api/admin/users/{publicId}/ban：封禁账号，传 type 为 TEMPORARY 或 PERMANENT，临时封禁要传天数，理由会记进审计日志
- POST /api/admin/users/{publicId}/unban：解封账号，理由会记进审计日志
- GET /api/admin/reports：举报列表，分页返回，包括帖子举报和评论举报
- POST /api/admin/reports/{id}/handle：标记举报已处理，理由会记进审计日志
- GET /api/admin/audit-logs：审计日志列表，分页返回

## 显示规则

- 帖子 anonymous 是 true 时，前端和接口都只返回匿名用户，不返回真实昵称、公开 ID 和学号。
- 帖子 anonymous 是 false 时，显示注册昵称和公开 ID，不显示学号。
- 任何公开接口都不返回 password_hash、学号和数据库主键。
- 本人帖子会多一个是本人的标记，只有本人登录后才能看到这个标记，别人看不到。

## 权限规则

- 所有写操作都以令牌里的公开 ID 为准，不接受请求里传来的 userId 来冒充别人。
- 编辑和删除自己的帖子时，后端会核对帖子作者和当前登录用户是否一致，不一致直接返回无权限。
- 管理员操作只看角色，不看前端是否显示了管理入口。

- GET /api/announcements：公告列表，不用登录也能看，最新在最前面；POST /api/announcements 和 DELETE /api/announcements/{id}：只有管理员能发和删公告
- GET /api/me/favorites：当前用户收藏的帖子，分页返回，已删除的帖子不返回
- GET /api/me/profile：还返回联系方式 contactInfo 和当前选的密保问题；PATCH /api/me/profile：改昵称和联系方式，联系方式随时能改、不受昵称 30 天冷却限制
- PATCH /api/me/password 和 PATCH /api/me/security-question：原密码输错返回 400，原密码错误
- DELETE /api/auth/me：注销账号时学号保留原学号再加 _delete，密码、联系方式和密保清空，自己的帖子和评论改为匿名

- POST /api/uploads：传图片，单张不超过 5MB；Spring 的上传上限也在 application.yml 里配成了单文件 5MB、单次请求 10MB，不然超过 1MB 默认上限的图片根本到不了上传接口
- 帖子状态只在前端翻译成汉语显示：NORMAL 是正常，DELETED 按删帖人分已被用户删除 / 已被管理员删除（老数据没记删帖人时只显示已删除）；用户状态：ACTIVE 是正常，BANNED 按有没有截止时间分临时封禁 / 永久封禁，DELETED 是已注销；举报状态：PENDING 是待处理，HANDLED 是已处理
- GET /api/admin/users/{publicId}/identity：管理员按帖子查作者学号也走这个接口，匿名帖子和已注销用户都能查，注销用户的学号会去掉 _delete 后缀再返回
