import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const user = localStorage.getItem('user')
    if (user) {
      config.headers['Authorization'] = 'Bearer ' + JSON.parse(user).id
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    if (error.response) {
      const data = error.response.data
      ElMessage.error(data?.message || error.message || '网络错误')
    } else {
      ElMessage.error(error.message || '网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
