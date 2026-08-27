import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { isLoggedIn } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
    {
      path: '/users',
      name: 'users',
      component: () => import('../views/UsersView.vue')
    },
    {
      path: '/mybatis-users',
      name: 'mybatis-users',
      component: () => import('../views/MybatisUsersView.vue')
    }
  ]
})

// 全局路由守卫：未登录只能访问 /login，已登录访问 /login 自动回首页
router.beforeEach((to) => {
  if (!to.meta.public && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && isLoggedIn()) {
    return { path: '/' }
  }
  return true
})

export default router
