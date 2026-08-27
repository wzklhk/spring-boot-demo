import axios from 'axios'
import { getToken, clearAuth } from '../utils/auth'

// 后端统一响应结构：{ code, message, data }，code=200/201 视为成功
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动携带 JWT
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

// 响应拦截器：解包 Result + 401 统一跳登录
http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && (body.code === 200 || body.code === 201)) {
      return body.data
    }
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (err) => {
    if (err.response?.status === 401) {
      clearAuth()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    const msg = err.response?.data?.message || err.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

// prefix: '' = JPA 版 (/api/users)，'/mybatis' = MyBatis 版 (/api/mybatis/users)
export function listUsers(prefix = '') {
  return http.get(`${prefix}/users`)
}

export function getUserById(prefix, id) {
  return http.get(`${prefix}/users/${id}`)
}

export function getUserByUsername(username) {
  return http.get(`/mybatis/users/username/${username}`)
}

export function createUser(prefix, data) {
  return http.post(`${prefix}/users`, data)
}

export function updateUser(prefix, id, data) {
  return http.put(`${prefix}/users/${id}`, data)
}

export function deleteUser(prefix, id) {
  return http.delete(`${prefix}/users/${id}`)
}
