# 部署说明

第一版先本地跑通，云部署是下一步，不用一开始就上云。

## 本地运行

1. 起 MySQL：docker compose up -d db
2. 后端：cd backend，然后 mvn spring-boot:run
3. 前端：cd frontend，然后 npm install、npm run dev

## 上云之前必须做的事

- 把 JWT_SECRET 换成足够长的随机字符串，不要用 .env.example 里的占位值。
- 数据库账号不要用 root，单独建一个只有 campus_wall 库权限的账号。
- uploads 目录不要和代码一起公开，图片访问只走后端给出的地址，敏感图片以后可以再加鉴权。
- 给后端加上 HTTPS，后面再接域名。
- 定期备份 MySQL，备份文件不要放进 Git 仓库。

## 云部署可选路线

- 数据库：云厂商的 MySQL，或者自己服务器上的 Docker MySQL。
- 后端：用 backend/Dockerfile 打包成镜像跑起来，环境变量传数据库地址和 JWT 密钥。
- 前端：用 frontend/Dockerfile 构建静态文件，用 Nginx 对外提供，接口反向代理到后端。

## 第一个管理员账号

数据库里没有默认管理员，本地先注册一个普通账号，然后直接在数据库把这个账号的 role 改成 ADMIN，再用这个账号登录管理后台。不要把管理员账号密码写进代码和文档。
