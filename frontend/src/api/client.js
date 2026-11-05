// 校园墙项目
// 接口请求封装：统一走相对地址，请求发出前自动从本地存储里取出令牌放到请求头里。
import axios from 'axios'

const client = axios.create({
  baseURL: '/api'
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus_wall_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 令牌过期或被注销、封禁后，后端会返回 401，前端在这里清除本地令牌并回到登录页，避免用失效令牌继续请求。
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('campus_wall_token')
      localStorage.removeItem('campus_wall_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
