// 校园墙项目
// 前端入口：创建 Vue 应用并挂载路由，整个页面从这里启动。
import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index.js'

createApp(App).use(router).mount('#app')
