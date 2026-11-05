# 数据库设计

校区在平台内区分，学校本身只有一个：中国民用航空飞行学院。校区不是简单的分类标签，而是帖子的核心字段：每条帖子必须有校区，筛选和订阅都围绕校区做。

## campuses 校区

- id：主键
- name：校区名称，例如广汉校区
- code：校区编码，唯一
- description：校区说明，可为空

## users 用户

- id：主键，只在数据库内部做关联，不出现在公开接口里
- public_id：公开 ID，随机生成，与学号没有任何推导关系，公开接口只返回这个
- student_id：学号，唯一，只用来登录，第一版不做学校系统核验，注销后会被替换成不可恢复的占位值
- nickname：昵称，发帖选择非匿名时显示这个昵称
- password_hash：BCrypt 哈希，不存明文
- token_version：令牌版本号，注销或封禁后递增，旧令牌会立刻失效
- role：USER、ADMIN
- status：ACTIVE、BANNED、DELETED，封禁以账号为主
- ban_until：临时封禁截止时间，永久封禁时为空，到期后自动恢复
- ban_reason：封禁理由，会记进审计日志
- campus_id：所属校区
- avatar_url：头像地址，可为空，匿名帖子不展示头像
- created_at：注册时间

## user_campus_subscriptions 校区订阅

- id：主键
- user_id：订阅人
- campus_id：订阅的校区
- user_id 和 campus_id 组合唯一，同一个校区只能订阅一次
- created_at：订阅时间

## posts 帖子

- id：主键
- campus_id：所属校区，必填，筛选和订阅的核心字段
- category：LOST、SECONDHAND、QA、VENT
- author_id：作者，匿名帖子也保留这个字段，方便管理员追责，但公开接口不暴露
- title：标题
- content：正文，出现疑似手机号和身份证号格式时会被拦截
- anonymous：是否匿名，匿名只是一个展示开关，不是独立身份
- contact_info：联系方式，二手和失物常用，是用户主动留的，正文检测不针对这个字段
- status：NORMAL、DELETED，管理员删帖改为 DELETED，不物理删除
- ip_hash：发帖网络地址的哈希值，只用于频率统计和辅助风控，不存原始地址，也不作为唯一封禁依据
- created_at、updated_at

## lost_details 失物招领专属字段

- post_id：主键，同时是 posts 的外键
- kind：LOST 表示丢了东西，FOUND 表示捡到东西
- location：地点
- happened_at：发生时间

## secondhand_details 二手交易专属字段

- post_id：主键，同时是 posts 的外键
- price：价格
- condition_text：成色，例如九成新、有轻微划痕

## qa_details 提问求助专属字段

- post_id：主键，同时是 posts 的外键
- solved：是否已解决
- accepted_comment_id：被采纳的评论，可为空

## vent_details 吐槽专属字段

- post_id：主键，同时是 posts 的外键
- mood：心情标签，可为空，方便以后扩展

## post_images 帖子图片

- id：主键
- post_id：帖子
- image_url：图片访问地址，文件存在本地 uploads 目录，上线后可以换成对象存储
- sort_order：排序

## comments 评论

- id：主键
- post_id：帖子
- author_id：作者，匿名评论同样保留作者，但公开接口不暴露
- content：内容，出现疑似手机号和身份证号格式时会被拦截
- anonymous：是否匿名
- status：NORMAL、DELETED，管理员删评论改为 DELETED
- created_at

## post_likes 点赞

- id：主键
- post_id、user_id 组合唯一，一个用户对一个帖子只能点赞一次，数据库唯一约束是最后一道防线

## favorites 收藏

- id：主键
- post_id、user_id 组合唯一

## reports 举报

- id：主键
- reporter_id：举报人，只保留在内部，不公开
- post_id：被举报帖子，举报评论时也保存评论所属的帖子
- comment_id：被举报评论，举报帖子时为空
- target_type：POST 表示帖子，COMMENT 表示评论
- reason：举报原因
- status：PENDING、HANDLED
- created_at

## admin_audit_logs 管理员审计日志

- id：主键
- admin_id：操作管理员
- action：操作类型，例如 DELETE_POST、BAN_USER、VIEW_IDENTITY
- target_type：目标类型
- target_id：目标主键
- reason：操作理由
- created_at：操作时间
- 日志里只保存管理员公开 ID，不保存学号
