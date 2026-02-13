<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="归属用户">
          <el-select
            v-model="searchForm.ownerUserId"
            class="gemini-select"
            placeholder="全部"
            clearable
            filterable
            style="width: 220px"
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
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="gemini-select"
            placeholder="全部"
            clearable
            style="width: 160px"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="正常" />
            <el-option :value="0" label="已撤销" />
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
            新增 API Key
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accessKey" label="Access Key" min-width="180" />
        <el-table-column label="归属用户" min-width="160">
          <template #default="{ row }">
            {{ resolveUserLabel(row.ownerUserId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" style="border: none">
              {{ row.status === 1 ? '正常' : '已撤销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限范围" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatScopes(row.scopes) }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.expireAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近使用" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastUsedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canWrite" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              class="action-btn warning"
              :disabled="row.status !== 1"
              @click="handleRevoke(row)"
            >
              撤销
            </el-button>
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
      v-model="createDialogVisible"
      title="新增 API Key"
      width="560px"
      class="gemini-dialog"
      align-center
    >
      <el-form label-width="100px">
        <el-form-item label="归属用户">
          <el-select
            v-model="createForm.ownerUserId"
            class="gemini-select"
            placeholder="默认当前登录用户"
            clearable
            filterable
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
        <el-form-item label="权限范围">
          <el-input
            v-model="createForm.scopes"
            class="gemini-input"
            placeholder="多个权限用英文逗号分隔，例如 user:read,user:write"
          />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="createForm.expireAt"
            type="datetime"
            class="gemini-input"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitCreate">
          创建
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="secretDialogVisible"
      title="API Key 创建成功"
      width="620px"
      class="gemini-dialog"
      align-center
    >
      <el-alert
        title="请立即保存 Secret"
        type="warning"
        :closable="false"
        description="Secret 仅在本次创建结果返回一次，关闭后无法再次查看。"
        style="margin-bottom: 12px"
      />
      <el-descriptions :column="1" border class="gemini-descriptions">
        <el-descriptions-item label="Access Key">
          <span class="mono-text">{{ createdKey?.accessKey || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Secret">
          <span class="mono-text">{{ createdKey?.secret || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" class="gemini-btn-primary" @click="secretDialogVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createIdentityApiKey,
  listIdentityApiKeys,
  listIdentityUsers,
  revokeIdentityApiKey
} from '@/api/identity'
import { usePermission } from '@/composables/usePermission'
import type { IdentityApiKey, IdentityApiKeyCreateResult, IdentityUser } from '@/types/entity'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<IdentityApiKey[]>([])
const userOptions = ref<IdentityUser[]>([])
const { hasPermission } = usePermission()
const canWrite = computed(() => hasPermission('user:write'))

const searchForm = reactive({
  ownerUserId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const createDialogVisible = ref(false)
const secretDialogVisible = ref(false)

const createForm = reactive({
  ownerUserId: undefined as number | undefined,
  scopes: '',
  expireAt: ''
})

const createdKey = ref<IdentityApiKeyCreateResult | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listIdentityApiKeys({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ownerUserId: searchForm.ownerUserId,
      status: searchForm.status
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取 API Key 列表失败')
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

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.ownerUserId = undefined
  searchForm.status = undefined
  pagination.pageNum = 1
  fetchData()
}

const handleAdd = () => {
  createForm.ownerUserId = undefined
  createForm.scopes = ''
  createForm.expireAt = ''
  createDialogVisible.value = true
}

const handleSubmitCreate = async () => {
  submitLoading.value = true
  try {
    const scopes = createForm.scopes
      .split(',')
      .map(item => item.trim())
      .filter(item => item)

    const res = await createIdentityApiKey({
      ownerUserId: createForm.ownerUserId,
      scopes: scopes.length > 0 ? scopes : undefined,
      expireAt: createForm.expireAt || undefined
    })

    createdKey.value = res.data
    createDialogVisible.value = false
    secretDialogVisible.value = true
    ElMessage.success('API Key 创建成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '创建 API Key 失败')
  } finally {
    submitLoading.value = false
  }
}

const handleRevoke = async (row: IdentityApiKey) => {
  try {
    await ElMessageBox.confirm(`确定要撤销 API Key ${row.accessKey} 吗？`, '确认撤销', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await revokeIdentityApiKey({ id: row.id })
    ElMessage.success('撤销成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤销失败')
    }
  }
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const resolveUserLabel = (userId: number) => {
  const target = userOptions.value.find(item => item.id === userId)
  if (!target) {
    return `用户-${userId}`
  }
  return `${target.displayName} (${target.username})`
}

const formatScopes = (value?: string) => {
  if (!value) return '-'
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) {
      return parsed.join('、')
    }
    return value
  } catch (e) {
    return value
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchUsers()
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.gemini-descriptions {
  --el-descriptions-table-border: 1px solid var(--gemini-border);
  --el-descriptions-item-bordered-label-background: rgba(255, 255, 255, 0.05);
}

.mono-text {
  font-family: monospace;
  word-break: break-all;
}
</style>
