// 校园墙项目
// 前端路由：首页、帖子详情、发帖、我的帖子、登录、注册和管理后台的地址映射。
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import PublishView from '../views/PublishView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import ForgotPasswordView from '../views/ForgotPasswordView.vue'
import AdminView from '../views/AdminView.vue'
import MyPostsView from '../views/MyPostsView.vue'
import MyProfileView from '../views/MyProfileView.vue'
import SearchView from '../views/SearchView.vue'
import AnnouncementsView from '../views/AnnouncementsView.vue'
import MyFavoritesView from '../views/MyFavoritesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/post/:id', component: PostDetailView },
    { path: '/publish', component: PublishView },
    { path: '/my', component: MyPostsView },
    { path: '/profile', component: MyProfileView },
    { path: '/search', component: SearchView },
    { path: '/announcements', component: AnnouncementsView },
    { path: '/favorites', component: MyFavoritesView },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/forgot-password', component: ForgotPasswordView },
    { path: '/admin', component: AdminView }
  ]
})

export default router
