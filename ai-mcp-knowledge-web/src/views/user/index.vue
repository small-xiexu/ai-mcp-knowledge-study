<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            class="gemini-input"
            placeholder="请输入用户名关键词"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="gemini-select"
            placeholder="全部"
            clearable
            style="width: 150px"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
            <el-option :value="2" label="锁定" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="gemini-btn-primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button class="gemini-btn-secondary" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button v-if="canWrite" type="primary" class="gemini-btn-primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="displayName" label="显示名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="140">
          <template #default="{ row }">
            {{ row.mobile || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" style="border: none">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超管" width="90">
          <template #default="{ row }">
            <el-tag :type="row.superAdmin ? 'warning' : 'info'" effect="dark" style="border: none">
              {{ row.superAdmin ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button text class="action-btn" @click="handleEdit(row)">编辑</el-button>
              <el-button text class="action-btn" @click="handleOpenResetPassword(row)">重置密码</el-button>
              <el-button text class="action-btn" @click="handleGrantRole(row)">分配角色</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="userDialogVisible"
      :title="userForm.id ? '编辑用户' : '新增用户'"
      width="620px"
      class="gemini-dialog"
      align-center
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="95px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            class="gemini-input"
            :disabled="Boolean(userForm.id)"
            placeholder="3-64位，建议英文+数字"
          />
        </el-form-item>
        <el-form-item label="显示名" prop="displayName">
          <el-input v-model="userForm.displayName" class="gemini-input" placeholder="请输入显示名" />
        </el-form-item>
        <el-form-item v-if="!userForm.id" label="登录密码" prop="password">
          <el-input
            v-model="userForm.password"
            class="gemini-input"
            type="password"
            show-password
            placeholder="8-64位"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" class="gemini-input" placeholder="可选" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.mobile" class="gemini-input" placeholder="可选" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select
            v-model="userForm.status"
            class="gemini-select"
            style="width: 100%"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
            <el-option :value="2" label="锁定" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台超管">
          <el-switch v-model="userForm.superAdmin" class="gemini-switch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitUser">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetDialogVisible"
      :title="`重置密码 - ${resetTargetUser?.displayName || ''}`"
      width="520px"
      class="gemini-dialog"
      align-center
    >
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="110px">
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetForm.password"
            class="gemini-input"
            type="password"
            show-password
            placeholder="8-64位"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            class="gemini-input"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitResetPassword">
          确认重置
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="grantDialogVisible"
      :title="`分配角色 - ${currentUser?.displayName || ''}`"
      width="700px"
      class="gemini-dialog"
      align-center
    >
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        description="当前接口为覆盖式分配。请勾选该用户最终应具备的角色集合。"
        style="margin-bottom: 12px"
      />
      <div v-loading="roleLoading || grantLoading" class="role-wrapper">
        <el-empty v-if="roleOptions.length === 0" description="暂无角色可分配" />
        <el-checkbox-group v-else v-model="grantForm.roleIds" class="role-grid">
          <el-checkbox v-for="item in roleOptions" :key="item.id" :label="item.id" class="role-item">
            <span>{{ item.roleName }}</span>
            <span class="role-code">{{ item.roleCode }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitGrant">
          保存分配
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createIdentityUser,
  getIdentityUserRoleIds,
  grantIdentityUserRoles,
  listIdentityRoles,
  listIdentityUsers,
  resetIdentityUserPassword,
  updateIdentityUser
} from '@/api/identity'
import { usePermission } from '@/composables/usePermission'
import type { IdentityRole, IdentityUser } from '@/types/entity'

const loading = ref(false)
const submitLoading = ref(false)
const roleLoading = ref(false)
const grantLoading = ref(false)
const tableData = ref<IdentityUser[]>([])
const roleOptions = ref<IdentityRole[]>([])
const { hasPermission } = usePermission()
const canWrite = computed(() => hasPermission('user:write'))

const searchForm = reactive({
  username: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const userDialogVisible = ref(false)
const resetDialogVisible = ref(false)
const grantDialogVisible = ref(false)

const userFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()
const currentUser = ref<IdentityUser | null>(null)
const resetTargetUser = ref<IdentityUser | null>(null)

const userForm = reactive({
  id: undefined as number | undefined,
  username: '',
  displayName: '',
  password: '',
  email: '',
  mobile: '',
  status: 1,
  superAdmin: false
})

const resetForm = reactive({
  userId: undefined as number | undefined,
  password: '',
  confirmPassword: ''
})

const grantForm = reactive({
  userId: undefined as number | undefined,
  roleIds: [] as number[]
})

const validateUserPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (userForm.id) {
    callback()
    return
  }
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8 || value.length > 64) {
    callback(new Error('密码长度需在8到64之间'))
    return
  }
  callback()
}

const validateResetConfirmPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== resetForm.password) {
    callback(new Error('两次输入密码不一致'))
    return
  }
  callback()
}

const userRules: FormRules<typeof userForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度需在3到64之间', trigger: 'blur' }
  ],
  displayName: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
  password: [{ validator: validateUserPassword, trigger: 'blur' }]
}

const resetRules: FormRules<typeof resetForm> = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度需在8到64之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateResetConfirmPassword, trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listIdentityUsers({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      username: searchForm.username || undefined,
      status: searchForm.status
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  roleLoading.value = true
  try {
    const res = await listIdentityRoles({
      pageNum: 1,
      pageSize: 100,
      status: 1
    })
    roleOptions.value = res.data.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色列表失败')
  } finally {
    roleLoading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchData()
}

const resetUserForm = () => {
  userForm.id = undefined
  userForm.username = ''
  userForm.displayName = ''
  userForm.password = ''
  userForm.email = ''
  userForm.mobile = ''
  userForm.status = 1
  userForm.superAdmin = false
  userFormRef.value?.clearValidate()
}

const handleAdd = () => {
  resetUserForm()
  userDialogVisible.value = true
}

const handleEdit = (row: IdentityUser) => {
  userForm.id = row.id
  userForm.username = row.username
  userForm.displayName = row.displayName
  userForm.password = ''
  userForm.email = row.email || ''
  userForm.mobile = row.mobile || ''
  userForm.status = row.status ?? 1
  userForm.superAdmin = Boolean(row.superAdmin)
  userDialogVisible.value = true
}

const resetPasswordForm = () => {
  resetForm.userId = undefined
  resetForm.password = ''
  resetForm.confirmPassword = ''
  resetFormRef.value?.clearValidate()
}

const handleOpenResetPassword = (row: IdentityUser) => {
  resetTargetUser.value = row
  resetPasswordForm()
  resetForm.userId = row.id
  resetDialogVisible.value = true
}

const handleSubmitUser = async () => {
  if (!userFormRef.value) {
    return
  }
  const valid = await userFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    if (userForm.id) {
      await updateIdentityUser({
        id: userForm.id,
        displayName: userForm.displayName,
        email: userForm.email || undefined,
        mobile: userForm.mobile || undefined,
        status: userForm.status,
        superAdmin: userForm.superAdmin
      })
      ElMessage.success('用户更新成功')
    } else {
      await createIdentityUser({
        username: userForm.username,
        displayName: userForm.displayName,
        password: userForm.password,
        email: userForm.email || undefined,
        mobile: userForm.mobile || undefined,
        status: userForm.status,
        superAdmin: userForm.superAdmin
      })
      ElMessage.success('用户创建成功')
    }
    userDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存用户失败')
  } finally {
    submitLoading.value = false
  }
}

const handleSubmitResetPassword = async () => {
  if (!resetFormRef.value || !resetForm.userId) {
    return
  }
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    await resetIdentityUserPassword({
      userId: resetForm.userId,
      password: resetForm.password
    })
    ElMessage.success('密码重置成功')
    resetDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    submitLoading.value = false
  }
}

const handleGrantRole = async (row: IdentityUser) => {
  currentUser.value = row
  grantForm.userId = row.id
  grantForm.roleIds = []
  grantDialogVisible.value = true
  grantLoading.value = true
  try {
    if (roleOptions.value.length === 0) {
      await fetchRoles()
    }
    const roleIdsRes = await getIdentityUserRoleIds({ userId: row.id })
    grantForm.roleIds = roleIdsRes.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户角色失败')
  } finally {
    grantLoading.value = false
  }
}

const handleSubmitGrant = async () => {
  if (!grantForm.userId) {
    ElMessage.warning('未识别用户ID')
    return
  }
  submitLoading.value = true
  try {
    await grantIdentityUserRoles({
      userId: grantForm.userId,
      roleIds: grantForm.roleIds
    })
    ElMessage.success('角色分配成功')
    grantDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '角色分配失败')
  } finally {
    submitLoading.value = false
  }
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const statusTagType = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  return 'info'
}

const statusText = (status?: number) => {
  if (status === 1) return '启用'
  if (status === 2) return '锁定'
  return '禁用'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchRoles()
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.action-buttons {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.role-wrapper {
  min-height: 150px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.role-item {
  margin-right: 0;
  padding: 10px 12px;
  border: 1px solid var(--gemini-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
}

.role-code {
  margin-left: 8px;
  font-size: 12px;
  color: var(--gemini-text-secondary);
}

:deep(.role-item .el-checkbox__label) {
  display: inline-flex;
  align-items: center;
}
</style>
