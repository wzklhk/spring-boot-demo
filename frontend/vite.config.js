import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发模式：Vite dev server (5173) 将 /api 代理到 Spring Boot (8080)
// 生产模式：构建产物输出到 dist/（不打包进 jar），由 Spring Boot 以 file: 路径静态托管
//          （deploy/docker-compose.app.yml 挂载 frontend/dist → /app/frontend）
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
    outDir: 'dist',
    emptyOutDir: true
  }
})
