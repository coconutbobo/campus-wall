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

export default client
