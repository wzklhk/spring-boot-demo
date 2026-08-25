<template>
  <!-- 登录页：全屏渲染，不带主布局 -->
  <router-view v-if="isLoginPage" />

  <!-- 主布局（登录后） -->
  <el-container v-else class="app-container">
    <!-- 顶部导航：深色 header + Vue 绿点缀 + 用户信息 + GitHub 链接 -->
    <el-header class="app-header">
      <div class="header-left">
        <img src="/favicon.svg" alt="logo" class="logo-img" />
        <span class="logo-text">Spring Boot <b>Demo</b></span>
        <el-tag class="stack-tag" type="success" effect="dark" size="small" round>
          Vue 3 · Spring Boot 3
        </el-tag>
      </div>
      <div class="header-right">
        <el-dropdown v-if="currentUser" trigger="click" @command="handleUserCommand">
          <span class="user-chip">
            <el-icon :size="15"><UserFilled /></el-icon>
            <span>{{ currentUser.username }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                邮箱：{{ currentUser.email || '-' }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <a href="https://github.com/wzklhk/spring-boot-demo" target="_blank" rel="noopener" class="github-link">
          <el-icon :size="16"><Github /></el-icon>
          <span>GitHub</span>
        </a>
      </div>
    </el-header>

    <el-container class="body-container">
      <!-- 左侧导航栏 -->
      <el-aside width="220px" class="app-aside">
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理（JPA）</span>
          </el-menu-item>
          <el-menu-item index="/mybatis-users">
            <el-icon><DataAnalysis /></el-icon>
            <span>用户管理（MyBatis）</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 底部：License / Copyright -->
    <el-footer class="app-footer">
      MIT License © 2026
      <a href="https://github.com/wzklhk" target="_blank" rel="noopener">wzklhk</a>
      · Spring Boot 3 + Vue 3 前后端分离示例
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUser, clearAuth } from './utils/auth'
import { logout } from './api/auth'

const route = useRoute()
const router = useRouter()

const isLoginPage = computed(() => route.path === '/login')
const activeMenu = computed(() => route.path)
const currentUser = computed(() => getUser())

async function handleUserCommand(command) {
  if (command === 'logout') {
    try {
      await logout()
    } catch {
      // 后端登出失败也继续前端清理
    }
    clearAuth()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.app-container {
  height: 100%;
}

/* ---- Header ---- */
.app-header {
  background: var(--header-dark);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px !important;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: 18px;
  letter-spacing: 0.5px;
}

.logo-text b {
  color: var(--vue-green);
}

.stack-tag {
  margin-left: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;
}

.user-chip:hover {
  color: #fff;
  background: rgba(65, 184, 131, 0.2);
}

.github-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}

.github-link:hover {
  color: #fff;
  background: rgba(65, 184, 131, 0.2);
  text-decoration: none;
}

/* ---- 主体布局 ---- */
.body-container {
  height: calc(100% - 96px);
}

.app-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.side-menu {
  border-right: none;
  height: 100%;
}

.side-menu .el-menu-item.is-active {
  background: var(--el-color-primary-light-9);
  border-right: 3px solid var(--vue-green);
  color: var(--vue-green);
  font-weight: 600;
}

.app-main {
  background: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}

/* ---- Footer ---- */
.app-footer {
  height: 36px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}
</style>
