<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="gemini-select"
            placeholder="全部"
            clearable
            style="width: 180px"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="gemini-btn-primary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button class="gemini-btn-secondary" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button v-if="canWrite" type="primary" class="gemini-btn-primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增组织
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orgCode" label="组织编码" min-width="140" />
        <el-table-column prop="orgName" label="组织名称" min-width="140" />
        <el-table-column prop="parentId" label="父组织" width="100">
          <template #default="{ row }">
            {{ row.parentId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="orgPath" label="组织路径" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" style="border: none">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button text class="action-btn" @click="handleEdit(row)">编辑</el-button>
              <el-button text class="action-btn" @click="handleBindUser(row)">绑定用户</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="orgDialogVisible"
      :title="orgForm.id ? '编辑组织' : '新增组织'"
      width="560px"
      class="gemini-dialog"
      align-center
    >
      <el-form ref="orgFormRef" :model="orgForm" :rules="orgRules" label-width="90px">
        <el-form-item label="组织编码" prop="orgCode">
          <el-input
            v-model="orgForm.orgCode"
            class="gemini-input"
            :disabled="Boolean(orgForm.id)"
            placeholder="例如：PAYMENT"
          />
        </el-form-item>
        <el-form-item label="组织名称" prop="orgName">
          <el-input v-model="orgForm.orgName" class="gemini-input" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="父组织ID">
          <el-input-number v-model="orgForm.parentId" :min="1" class="gemini-input-number" controls-position="right" />
        </el-form-item>
        <el-form-item label="组织路径">
          <el-input v-model="orgForm.orgPath" class="gemini-input" placeholder="例如：/PAYMENT" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="orgForm.status"
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
            v-model="orgForm.remark"
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
        <el-button class="gemini-btn-secondary" @click="orgDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitOrg">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindDialogVisible"
      :title="`绑定用户主组织 - ${currentOrg?.orgName || ''}`"
      width="520px"
      class="gemini-dialog"
      align-center
    >
      <el-form label-width="100px">
        <el-form-item label="选择用户">
          <el-select
            v-model="bindForm.userId"
            class="gemini-select"
            filterable
            clearable
            placeholder="请选择用户"
            style="width: 100%"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="`${item.displayName} (${item.username})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitBind">
          保存绑定
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
  bindIdentityUserOrg,
  createIdentityOrg,
  listIdentityOrgs,
  listIdentityUsers,
  updateIdentityOrg
} from '@/api/identity'
import { usePermission } from '@/composables/usePermission'
import type { IdentityOrg, IdentityUser } from '@/types/entity'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<IdentityOrg[]>([])
const userOptions = ref<IdentityUser[]>([])
const { hasPermission } = usePermission()
const canWrite = computed(() => hasPermission('user:write'))

const searchForm = reactive({
  status: undefined as number | undefined
})

const orgDialogVisible = ref(false)
const bindDialogVisible = ref(false)
const orgFormRef = ref<FormInstance>()
const currentOrg = ref<IdentityOrg | null>(null)

const orgForm = reactive({
  id: undefined as number | undefined,
  orgCode: '',
  orgName: '',
  parentId: undefined as number | undefined,
  orgPath: '',
  status: 1,
  remark: ''
})

const bindForm = reactive({
  orgId: undefined as number | undefined,
  userId: undefined as number | undefined
})

const orgRules: FormRules<typeof orgForm> = {
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listIdentityOrgs({
      status: searchForm.status
    })
    tableData.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取组织列表失败')
  } finally {
    loading.value = false
  }
}

const fetchUsers = async () => {
  try {
    const res = await listIdentityUsers({
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    userOptions.value = res.data.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取用户列表失败')
  }
}

const handleReset = () => {
  searchForm.status = undefined
  fetchData()
}

const resetOrgForm = () => {
  orgForm.id = undefined
  orgForm.orgCode = ''
  orgForm.orgName = ''
  orgForm.parentId = undefined
  orgForm.orgPath = ''
  orgForm.status = 1
  orgForm.remark = ''
  orgFormRef.value?.clearValidate()
}

const handleAdd = () => {
  resetOrgForm()
  orgDialogVisible.value = true
}

const handleEdit = (row: IdentityOrg) => {
  orgForm.id = row.id
  orgForm.orgCode = row.orgCode
  orgForm.orgName = row.orgName
  orgForm.parentId = row.parentId
  orgForm.orgPath = row.orgPath || ''
  orgForm.status = row.status ?? 1
  orgForm.remark = row.remark || ''
  orgDialogVisible.value = true
}

const handleSubmitOrg = async () => {
  if (!orgFormRef.value) {
    return
  }
  const valid = await orgFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitLoading.value = true
  try {
    if (orgForm.id) {
      await updateIdentityOrg({
        id: orgForm.id,
        orgName: orgForm.orgName,
        parentId: orgForm.parentId,
        orgPath: orgForm.orgPath || undefined,
        status: orgForm.status,
        remark: orgForm.remark || undefined
      })
      ElMessage.success('组织更新成功')
    } else {
      await createIdentityOrg({
        orgCode: orgForm.orgCode,
        orgName: orgForm.orgName,
        parentId: orgForm.parentId,
        orgPath: orgForm.orgPath || undefined,
        status: orgForm.status,
        remark: orgForm.remark || undefined
      })
      ElMessage.success('组织创建成功')
    }
    orgDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存组织失败')
  } finally {
    submitLoading.value = false
  }
}

const handleBindUser = async (row: IdentityOrg) => {
  currentOrg.value = row
  bindForm.orgId = row.id
  bindForm.userId = undefined
  bindDialogVisible.value = true
  if (userOptions.value.length === 0) {
    await fetchUsers()
  }
}

const handleSubmitBind = async () => {
  if (!bindForm.orgId || !bindForm.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  submitLoading.value = true
  try {
    await bindIdentityUserOrg({
      orgId: bindForm.orgId,
      userId: bindForm.userId
    })
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '绑定失败')
  } finally {
    submitLoading.value = false
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchData()
  fetchUsers()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
</style>
