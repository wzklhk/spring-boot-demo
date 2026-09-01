import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发模式：Vite dev server (5173) 将 /api 代理到 Spring Boot (9090, dev profile 端口)
// 生产模式：frontend-maven-plugin 在 `mvn package` 时执行本构建，
//           产物输出到 spring-boot/src/main/resources/static，随后端一并打进 jar
//           （classpath:/static/），最终 JAR 自包含前端，无需外部 vue/dist 挂载
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
    outDir: '../spring-boot/src/main/resources/static',
    emptyOutDir: true
  }
})
