import axios from 'axios'

// 认证接口（/api/auth/* 无需 token）
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

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

export function register(data) {
  return http.post('/auth/register', data)
}

export function logout() {
  return http.post('/auth/logout')
}

export function deleteAccount() {
  return http.delete('/auth/account')
}
