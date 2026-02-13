<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色编码">
          <el-input
            v-model="searchForm.roleCode"
            class="gemini-input"
            placeholder="请输入角色编码关键词"
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
            style="width: 160px"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
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
            新增角色
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleScope" label="范围" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" style="border: none">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button text class="action-btn" @click="handleEdit(row)">编辑</el-button>
              <el-button text class="action-btn" @click="handleGrantPermission(row)">授权</el-button>
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
      v-model="roleDialogVisible"
      :title="roleForm.id ? '编辑角色' : '新增角色'"
      width="560px"
      class="gemini-dialog"
      align-center
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="roleForm.roleCode"
            class="gemini-input"
            :disabled="Boolean(roleForm.id)"
            placeholder="例如：OPS_ADMIN"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" class="gemini-input" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色范围">
          <el-select
            v-model="roleForm.roleScope"
            class="gemini-select"
            placeholder="请选择范围"
            style="width: 100%"
            popper-class="gemini-select-dropdown"
          >
            <el-option label="租户" value="TENANT" />
            <el-option label="全局" value="GLOBAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="roleForm.status"
            class="gemini-select"
            style="width: 100%"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="roleForm.remark"
            class="gemini-input"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitRole">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="grantDialogVisible"
      :title="`角色授权 - ${currentRole?.roleName || ''}`"
      width="720px"
      class="gemini-dialog"
      align-center
    >
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        description="当前接口为覆盖式授权，请勾选该角色最终应具备的完整权限集合。"
        style="margin-bottom: 12px"
      />
      <div v-loading="permissionLoading || grantLoading" class="permission-wrapper">
        <el-empty v-if="permissionOptions.length === 0" description="暂无权限定义" />
        <el-checkbox-group v-else v-model="grantForm.permissionIds" class="permission-grid">
          <el-checkbox v-for="item in permissionOptions" :key="item.id" :label="item.id" class="permission-item">
            <span>{{ item.permissionName }}</span>
            <span class="permission-code">{{ item.permissionCode }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitGrant">
          保存授权
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
  createIdentityRole,
  getIdentityRolePermissionIds,
  grantIdentityRolePermissions,
  listIdentityPermissions,
  listIdentityRoles,
  updateIdentityRole
} from '@/api/identity'
import { usePermission } from '@/composables/usePermission'
import type { IdentityPermission, IdentityRole } from '@/types/entity'

const loading = ref(false)
const submitLoading = ref(false)
const permissionLoading = ref(false)
const grantLoading = ref(false)
const tableData = ref<IdentityRole[]>([])
const permissionOptions = ref<IdentityPermission[]>([])
const { hasPermission } = usePermission()
const canWrite = computed(() => hasPermission('role:write'))

const searchForm = reactive({
  roleCode: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const roleDialogVisible = ref(false)
const grantDialogVisible = ref(false)
const roleFormRef = ref<FormInstance>()
const currentRole = ref<IdentityRole | null>(null)

const roleForm = reactive({
  id: undefined as number | undefined,
  roleCode: '',
  roleName: '',
  roleScope: 'TENANT',
  status: 1,
  remark: ''
})

const grantForm = reactive({
  roleId: undefined as number | undefined,
  permissionIds: [] as number[]
})

const roleRules: FormRules<typeof roleForm> = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_][A-Z0-9_]{1,63}$/, message: '角色编码需为大写字母/数字/下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listIdentityRoles({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      roleCode: searchForm.roleCode || undefined,
      status: searchForm.status
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const fetchPermissions = async () => {
  permissionLoading.value = true
  try {
    const res = await listIdentityPermissions({
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    permissionOptions.value = res.data.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取权限列表失败')
  } finally {
    permissionLoading.value = false
  }
}

const resetRoleForm = () => {
  roleForm.id = undefined
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.roleScope = 'TENANT'
  roleForm.status = 1
  roleForm.remark = ''
  roleFormRef.value?.clearValidate()
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.roleCode = ''
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchData()
}

const handleAdd = () => {
  resetRoleForm()
  roleDialogVisible.value = true
}

const handleEdit = (row: IdentityRole) => {
  roleForm.id = row.id
  roleForm.roleCode = row.roleCode
  roleForm.roleName = row.roleName
  roleForm.roleScope = row.roleScope || 'TENANT'
  roleForm.status = row.status ?? 1
  roleForm.remark = row.remark || ''
  roleDialogVisible.value = true
}

const handleSubmitRole = async () => {
  if (!roleFormRef.value) {
    return
  }
  const valid = await roleFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    if (roleForm.id) {
      await updateIdentityRole({
        id: roleForm.id,
        roleName: roleForm.roleName,
        roleScope: roleForm.roleScope,
        status: roleForm.status,
        remark: roleForm.remark || undefined
      })
      ElMessage.success('角色更新成功')
    } else {
      await createIdentityRole({
        roleCode: roleForm.roleCode,
        roleName: roleForm.roleName,
        roleScope: roleForm.roleScope,
        status: roleForm.status,
        remark: roleForm.remark || undefined
      })
      ElMessage.success('角色创建成功')
    }
    roleDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存角色失败')
  } finally {
    submitLoading.value = false
  }
}

const handleGrantPermission = async (row: IdentityRole) => {
  currentRole.value = row
  grantForm.roleId = row.id
  grantForm.permissionIds = []
  grantDialogVisible.value = true
  grantLoading.value = true
  try {
    if (permissionOptions.value.length === 0) {
      await fetchPermissions()
    }
    const permissionIdsRes = await getIdentityRolePermissionIds({ roleId: row.id })
    grantForm.permissionIds = permissionIdsRes.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色权限失败')
  } finally {
    grantLoading.value = false
  }
}

const handleSubmitGrant = async () => {
  if (!grantForm.roleId) {
    ElMessage.warning('未识别角色ID')
    return
  }
  submitLoading.value = true
  try {
    await grantIdentityRolePermissions({
      roleId: grantForm.roleId,
      permissionIds: grantForm.permissionIds
    })
    ElMessage.success('授权成功')
    grantDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '角色授权失败')
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

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.permission-wrapper {
  min-height: 160px;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.permission-item {
  margin-right: 0;
  padding: 10px 12px;
  border: 1px solid var(--gemini-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
}

.permission-code {
  margin-left: 8px;
  font-size: 12px;
  color: var(--gemini-text-secondary);
}

:deep(.permission-item .el-checkbox__label) {
  display: inline-flex;
  align-items: center;
}
</style>
