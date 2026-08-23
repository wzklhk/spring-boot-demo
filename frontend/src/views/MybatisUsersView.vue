<template>
  <div class="mybatis-view">
    <div class="page-header">
      <div>
        <h2>用户管理（MyBatis）</h2>
        <p class="sub">接口前缀 <code>/api/mybatis/users</code> · MyBatis XML Mapper 实现</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>
        新建用户
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <!-- 按用户名查询（MyBatis 专属接口） -->
      <div class="search-bar">
        <el-input
          v-model="searchUsername"
          placeholder="输入用户名精确查询"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="fetchUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" plain @click="handleSearch">查询</el-button>
        <el-button v-if="searching" @click="resetSearch">显示全部</el-button>
      </div>

      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户，点击右上角「新建用户」创建" :image-size="80" />
        </template>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑用户' : '新建用户'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  deleteUser,
  getUserByUsername,
  listUsers,
  updateUser
} from '../api/user'

// MyBatis 版接口前缀
const PREFIX = '/mybatis'

const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const searchUsername = ref('')
const searching = ref(false)

const form = reactive({ id: null, username: '', email: '' })

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱不能为空', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function fetchUsers() {
  loading.value = true
  try {
    users.value = await listUsers(PREFIX)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  const name = searchUsername.value.trim()
  if (!name) {
    ElMessage.warning('请输入要查询的用户名')
    return
  }
  loading.value = true
  try {
    const user = await getUserByUsername(name)
    users.value = user ? [user] : []
    searching.value = true
    if (!user) ElMessage.info('未找到该用户')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchUsername.value = ''
  searching.value = false
  fetchUsers()
}

function openCreate() {
  form.id = null
  form.username = ''
  form.email = ''
  dialogVisible.value = true
}

function openEdit(row) {
  form.id = row.id
  form.username = row.username
  form.email = row.email
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateUser(PREFIX, form.id, { username: form.username, email: form.email })
      ElMessage.success('更新成功')
    } else {
      await createUser(PREFIX, { username: form.username, email: form.email })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    searching.value = false
    searchUsername.value = ''
    await fetchUsers()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户取消
  }
  try {
    await deleteUser(PREFIX, row.id)
    ElMessage.success('删除成功')
    if (searching.value && searchUsername.value.trim()) {
      await handleSearch()
    } else {
      await fetchUsers()
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function formatTime(t) {
  return t ? t.replace('T', ' ') : '-'
}

onMounted(fetchUsers)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1a1a2e;
}

.sub {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.sub code {
  background: var(--el-color-primary-light-9);
  color: var(--vue-green);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.table-card {
  border-radius: 10px;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
