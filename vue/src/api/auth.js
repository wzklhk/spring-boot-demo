import axios from 'axios'
import { getToken } from '../utils/auth'

// 认证接口：/api/auth/login 公开；其余（logout/password/account）需携带 token
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：登录外的认证接口自动携带 JWT
http.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && (body.code === 200 || body.code === 201)) {
      return body.data
    }
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export function login(data) {
  return http.post('/auth/login', data)
}

export function changePassword(data) {
  return http.post('/auth/password', data)
}

export function logout() {
  return http.post('/auth/logout')
}

export function deleteAccount() {
  return http.delete('/auth/account')
}
