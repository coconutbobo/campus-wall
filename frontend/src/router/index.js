// 校园墙项目
// 前端路由：首页、帖子详情、发帖、登录、注册和管理后台六个页面的地址映射。
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import PublishView from '../views/PublishView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AdminView from '../views/AdminView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/post/:id', component: PostDetailView },
    { path: '/publish', component: PublishView },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/admin', component: AdminView }
  ]
})

export default router
