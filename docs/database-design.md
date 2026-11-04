# 数据库设计

校区在平台内区分，学校本身只有一个：中国民用航空飞行学院。

## campuses 校区

- id：主键
- name：校区名称，例如广汉校区
- code：校区编码，唯一

## users 用户

- id：主键
- student_id：学号，唯一，登录账号
- nickname：昵称，发帖选择非匿名时显示这个昵称
- password_hash：BCrypt 哈希，不存明文
- role：USER、ADMIN
- status：ACTIVE、BANNED，封禁以账号为主
- campus_id：所属校区
- avatar_url：头像地址，可为空
- created_at：注册时间

## posts 帖子

- id：主键
- campus_id：所属校区
- category：LOST、SECONDHAND、QA、VENT
- author_id：作者
- title：标题
- content：正文
- anonymous：是否匿名，匿名显示为匿名用户
- contact_info：联系方式，二手和失物常用
- status：NORMAL、DELETED，管理员删帖改为 DELETED，不物理删除
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
- author_id：作者
- content：内容
- anonymous：是否匿名
- created_at

## post_likes 点赞

- id：主键
- post_id、user_id 组合唯一，一个用户对一个帖子只能点赞一次

## favorites 收藏

- id：主键
- post_id、user_id 组合唯一

## reports 举报

- id：主键
- reporter_id：举报人
- post_id：被举报帖子
- reason：举报原因
- status：PENDING、HANDLED
- created_at
