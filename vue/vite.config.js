import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发模式：Vite dev server (5173) 将 /api 代理到 Spring Boot (9090, dev profile 端口)
// 生产模式：构建产物输出到 dist/（不打包进 jar），由 Spring Boot 以 file: 路径静态托管
//          （deploy/docker-compose.app.yml 挂载 vue/dist → /app/frontend）
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': 'http://localhost:9090',
      '/h2-console': 'http://localhost:9090',
      // Knife4j 文档（也可直接访问后端 http://localhost:9090/doc.html）
      '/doc.html': 'http://localhost:9090',
      '/swagger-ui': 'http://localhost:9090',
      '/v3/api-docs': 'http://localhost:9090',
      '/webjars': 'http://localhost:9090'
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
})
