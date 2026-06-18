import axios from 'axios'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  config => {
    const user = localStorage.getItem('m_user')
    if (user) config.headers['Authorization'] = 'Bearer ' + JSON.parse(user).id
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    showToast(msg)
    return Promise.reject(error)
  }
)

export default request
