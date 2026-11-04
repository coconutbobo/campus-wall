# campus-wall 校园墙

README 不按校区拆分学校介绍，平台内部按校区区分内容，用户发帖和浏览时可以选择校区。

## 第一版板块

- 失物招领
- 二手交易
- 提问求助
- 吐槽

## 核心规则

- 不登录可以浏览，登录后可以发帖、评论、点赞、收藏、举报。
- 发帖可以选择显示昵称或者匿名，匿名统一显示为匿名用户。
- 帖子直接发布，不经过前置审核，管理员可以在后台删帖、处理举报、封禁账号，封禁以账号为主。
- 不做私信，不做站内支付，二手交易只发帖、留联系方式，线下自行交易。
- 支持上传图片，失物招领和二手交易建议配图。

## 技术栈

- 前端：Vue 3 + Vite + Vue Router + Axios
- 后端：Java 17 + Spring Boot 3 + Spring Data JPA + Spring Security + JWT
- 数据库：MySQL 8
- 本地运行：先本地跑通，后面再部署到云

## 目录结构

- backend：Spring Boot 后端
- frontend：Vue 3 前端
- docs：数据库设计、接口设计、部署说明

## 本地运行

### 1. 准备数据库

用 Docker 只起一个 MySQL，或者用自己已经装好的 MySQL：

```bash
docker compose up -d db
```

数据库名默认是 campus_wall，账号密码见 .env.example，本地不要把真实密码写进仓库。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认跑在 http://localhost:8080

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认跑在 http://localhost:5173，接口通过 Vite 代理转发到后端的 /api。

## 登录方式

用学号注册和登录，注册时需要填写昵称和选择校区，登录成功后拿到 JWT，之后发帖、评论等操作都要带上这个令牌。

密码只存 BCrypt 哈希，不存明文。

## 开源与用户数据保密

代码开源，但用户数据不开源，也不进仓库：

- 真实数据库连接、JWT 密钥、上传文件一律走环境变量和本地配置，不写进代码。
- .gitignore 已经排除了 .env、本地配置文件、uploads 目录和构建产物。
- 不要把真实学号、密码、联系方式、数据库备份提交到 GitHub。
- 上线前必须换掉默认 JWT 密钥，见 docs/deployment.md。
