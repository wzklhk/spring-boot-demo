<template>
  <div class="users-view">
    <div class="page-header">
      <div>
        <h2>用户管理（JPA）</h2>
        <p class="sub">接口前缀 <code>/api/user</code> · 统一分页查询 <code>POST /api/user/query</code></p>
      </div>
      <div class="header-actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>
          新建用户
        </el-button>
        <el-button plain @click="openPwdDialog">
          <el-icon style="margin-right: 4px"><Key /></el-icon>
          修改密码
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <!-- 按用户名条件查询（走统一分页查询 API，传 UserVO 条件） -->
      <div class="search-bar">
        <el-input
          v-model="searchUsername"
          placeholder="输入用户名查询"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="resetSearch"
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
        <el-table-column prop="createdBy" label="创建人" min-width="120">
          <template #default="{ row }">{{ row.createdBy || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" :image-size="80" />
        </template>
      </el-table>

      <!-- 查询接口均为分页查询：普通列表与条件查询共用同一分页器 -->
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @size-change="handleSizeChange"
        @current-change="fetchUsers"
      />
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
        <!-- 仅新建时设置初始密码；编辑不修改密码 -->
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password
                    placeholder="6-64 位，用于首次登录" autocomplete="new-password" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password
                    placeholder="再次输入初始密码" autocomplete="new-password"
                    @keyup.enter="handleSubmit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改当前登录用户密码对话框（POST /api/auth/password） -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="440px" destroy-on-close>
      <p class="pwd-tip">正在修改当前登录账号「{{ currentUser?.username || '-' }}」的密码</p>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password
                    placeholder="请输入原密码" autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password
                    placeholder="6-64 位新密码" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password
                    placeholder="再次输入新密码" autocomplete="new-password"
                    @keyup.enter="handlePwdSubmit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="handlePwdSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, deleteUser, queryUsers, updateUser } from '../api/user'
import { changePassword } from '../api/auth'
import { getUser } from '../utils/auth'

const users = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
// 仅 ADMIN 角色可新建/编辑/删除用户；其他角色只能查看
const currentUser = getUser()
const canManage = computed(() => Array.isArray(currentUser?.roles) && currentUser.roles.includes('ADMIN'))
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const searchUsername = ref('')
const searching = ref(false)

const form = reactive({ id: null, username: '', email: '', password: '', confirmPassword: '' })

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱不能为空', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置初始密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

// ---- 修改密码（当前登录账号） ----
const pwdDialogVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度需在 6-64 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        value === pwdForm.newPassword ? callback() : callback(new Error('两次输入的新密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

async function handlePwdSubmit() {
  await pwdFormRef.value.validate()
  pwdSubmitting.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请牢记新密码')
    pwdDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    pwdSubmitting.value = false
  }
}

// 统一分页查询：传空 VO {} 即普通分页；搜索时传 username 条件
async function fetchUsers() {
  loading.value = true
  try {
    const vo = searching.value ? { username: searchUsername.value.trim() } : {}
    const data = await queryUsers(vo, page.value, pageSize.value)
    users.value = data.list
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  fetchUsers()
}

async function handleSearch() {
  const name = searchUsername.value.trim()
  if (!name) {
    ElMessage.warning('请输入要查询的用户名')
    return
  }
  searching.value = true
  page.value = 1
  await fetchUsers()
}

function resetSearch() {
  searchUsername.value = ''
  searching.value = false
  page.value = 1
  fetchUsers()
}

function openCreate() {
  form.id = null
  form.username = ''
  form.email = ''
  form.password = ''
  form.confirmPassword = ''
  dialogVisible.value = true
}

function openEdit(row) {
  form.id = row.id
  form.username = row.username
  form.email = row.email
  form.password = ''
  form.confirmPassword = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateUser(form.id, { username: form.username, email: form.email })
      ElMessage.success('更新成功')
    } else {
      await createUser({
        username: form.username,
        email: form.email,
        password: form.password
      })
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
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await fetchUsers()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

// 后端返回 UTC 时间（带 Z），这里按浏览器本地时区显示
function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
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

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pwd-tip {
  margin: 0 0 12px;
  color: #909399;
  font-size: 13px;
}
</style>