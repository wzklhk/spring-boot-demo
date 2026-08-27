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

// 统一用户接口 /api/users（Service 多态，默认 MyBatis 实现，前端无感知）
// 列表接口默认分页：page 从 1 起，size 默认 10，返回 { list, total, page, size, pages }
export function listUsers(page = 1, size = 10) {
  return http.get('/users', { params: { page, size } })
}

export function getUserById(id) {
  return http.get(`/users/${id}`)
}

export function getUserByUsername(username) {
  return http.get(`/users/username/${username}`)
}

export function createUser(data) {
  return http.post('/users', data)
}

export function updateUser(id, data) {
  return http.put(`/users/${id}`, data)
}

export function deleteUser(id) {
  return http.delete(`/users/${id}`)
}
