<template>
  <div class="home">
    <!-- Hero 区：Vue 绿点缀 -->
    <div class="hero">
      <h1>Spring Boot 3 + Vue 3 前后端分离示例</h1>
      <p>
        一个完整的全栈 Demo：后端 Spring Boot 3（JPA + MyBatis 双持久层共存），
        前端 Vue 3 + Element Plus，单仓库 monorepo 组织。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" round @click="$router.push('/users')">
          <el-icon style="margin-right: 6px"><User /></el-icon>
          JPA 用户管理
        </el-button>
        <el-button size="large" round @click="$router.push('/mybatis-users')">
          <el-icon style="margin-right: 6px"><DataAnalysis /></el-icon>
          MyBatis 用户管理
        </el-button>
      </div>
    </div>

    <!-- 技术栈卡片 -->
    <el-row :gutter="16" class="cards">
      <el-col :xs="24" :sm="12" :md="8" v-for="card in techCards" :key="card.title">
        <el-card shadow="hover" class="tech-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" :color="'#41b883'"><component :is="card.icon" /></el-icon>
              <span>{{ card.title }}</span>
            </div>
          </template>
          <p class="card-desc">{{ card.desc }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 双持久层说明 -->
    <el-card shadow="never" class="dual-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="18" color="#41b883"><Connection /></el-icon>
          <span>双持久层设计（JPA + MyBatis）</span>
        </div>
      </template>
      <ul class="dual-list">
        <li>JPA 负责建表（<code>ddl-auto: update</code>），接口前缀 <code>/api/users</code></li>
        <li>MyBatis 通过 XML Mapper 读写同一张 <code>users</code> 表，接口前缀 <code>/api/mybatis/users</code></li>
        <li>两套 API 功能等价，可交叉验证：JPA 写入的数据 MyBatis 能读到，反之亦然</li>
        <li>事务统一由 Spring 管理（<code>@Transactional</code> 同时覆盖两套持久层）</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
const techCards = [
  {
    title: '后端 Spring Boot 3.5',
    icon: 'Cpu',
    desc: 'Jakarta EE + Java 21，RESTful API、参数校验、全局异常处理、统一响应格式 {code, message, data}。'
  },
  {
    title: '前端 Vue 3 + Vite',
    icon: 'Monitor',
    desc: '组合式 API + vue-router，Vite 开发代理热更新，生产构建打包进后端 static，单 jar 部署。'
  },
  {
    title: '双持久层共存',
    icon: 'Coin',
    desc: 'Spring Data JPA 与 MyBatis 3.0.4 共享同一 H2 数据源与 users 表，两套等价接口便于对比学习。'
  }
]
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 12px;
  color: #fff;
  padding: 48px 40px;
  margin-bottom: 24px;
}

.hero h1 {
  margin: 0 0 16px;
  font-size: 28px;
}

.hero h1::before {
  content: '';
  display: inline-block;
  width: 10px;
  height: 28px;
  background: var(--vue-green);
  border-radius: 3px;
  margin-right: 12px;
  vertical-align: -2px;
}

.hero p {
  color: rgba(255, 255, 255, 0.75);
  font-size: 15px;
  line-height: 1.8;
  margin: 0 0 28px;
  max-width: 640px;
}

.cards {
  margin-bottom: 24px;
}

.tech-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.card-desc {
  color: #606266;
  font-size: 13.5px;
  line-height: 1.8;
  margin: 0;
}

.dual-card {
  border-radius: 10px;
}

.dual-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  font-size: 14px;
  line-height: 2.2;
}

.dual-list code {
  background: var(--el-color-primary-light-9);
  color: var(--vue-green);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12.5px;
}
</style>
