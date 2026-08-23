import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发模式：Vite dev server (5173) 将 /api 代理到 Spring Boot (8080)
// 生产模式：构建产物直接输出到 backend/src/main/resources/static，
//          由 Spring Boot 单 jar 托管（前后端同源，无 CORS 问题）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/h2-console': 'http://localhost:8080'
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})
