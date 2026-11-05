# 部署说明

第一版先本地跑通，云部署是下一步，不用一开始就上云。

## 本地运行

1. 起 MySQL：docker compose up -d db，数据库只在 Docker 内部网络里，不对外映射端口。
2. 如果本地需要用数据库工具直连，用开发覆盖文件：docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d db，这个覆盖文件只把端口绑到 127.0.0.1，不会绑到公网接口。
3. 后端：cd backend，然后 mvn spring-boot:run
4. 前端：cd frontend，然后 npm install、npm run dev

## 整体用 Docker 部署

docker compose up -d 就可以把数据库、后端和前端都跑起来，数据库不对外映射端口，前端是唯一对外提供 80 端口的入口，后端只在内部网络里被前端反向代理访问。

没有写 container_name，Compose 会按项目名自动生成容器名，同一台服务器上可以用不同的项目名同时跑多个测试环境而不会命名冲突，例如用 docker compose -p campus-wall-test up -d。

## 上云之前必须做的事

- 把 JWT_SECRET 换成足够长的随机字符串，不要用 .env.example 里的占位值，生产环境绝不能用默认密钥，否则任何人都能伪造令牌。
- 数据库密码换成强密码，不要用 .env.example 里的占位密码，真实密码只放进服务器上的 .env，绝不提交到 GitHub，.env 已经在 .gitignore 里。
- 数据库不要对外映射端口，公网即使有密码也不建议直接访问数据库，数据库只在内部网络里提供服务。
- 数据库账号不要用 root，单独建一个只有 campus_wall 库权限的账号。
- uploads 目录不要和代码一起公开，图片访问只走后端给出的地址，敏感图片以后可以再加鉴权。
- 给前端加上 HTTPS，后面再接域名。
- 定期备份 MySQL，备份文件不要放进 Git 仓库。

## 云部署可选路线

- 数据库：云厂商的 MySQL，或者自己服务器上的 Docker MySQL，两种方式都不要对外暴露数据库端口。
- 后端：用 backend/Dockerfile 打包成镜像跑起来，环境变量传数据库地址和 JWT 密钥。
- 前端：用 frontend/Dockerfile 构建静态文件，用 Nginx 对外提供，接口反向代理到后端。

## 第一个管理员账号

数据库里没有默认管理员，本地先注册一个普通账号，然后直接在数据库把这个账号的 role 改成 ADMIN，再用这个账号登录管理后台。不要把管理员账号密码写进代码和文档。
