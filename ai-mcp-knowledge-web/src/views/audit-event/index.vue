<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作人">
          <el-input
            v-model="searchForm.operatorKeyword"
            class="gemini-input"
            placeholder="请输入用户名或ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="结果">
          <el-select
            v-model="searchForm.result"
            class="gemini-select"
            placeholder="全部"
            clearable
            style="width: 140px"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="成功" />
            <el-option :value="0" label="失败" />
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
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column label="操作人" width="180">
          <template #default="{ row }">
            {{ formatOperator(row) }}
          </template>
        </el-table-column>
        <el-table-column label="动作" width="110">
          <template #default="{ row }">
            {{ formatAction(row.action) }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'" effect="dark" style="border: none">
              {{ row.result === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源IP" width="140">
          <template #default="{ row }">
            {{ row.sourceIp || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" width="100">
          <template #default="{ row }">
            {{ row.costMs ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发生时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.occurredAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text class="action-btn" @click="handleViewDetail(row)">详情</el-button>
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
      v-model="detailDialogVisible"
      title="审计事件详情"
      width="760px"
      class="gemini-dialog"
      align-center
    >
      <el-descriptions :column="2" border class="gemini-descriptions">
        <el-descriptions-item label="操作人">{{ formatOperator(currentEvent) }}</el-descriptions-item>
        <el-descriptions-item label="操作主体类型">{{ currentEvent?.operatorType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ currentEvent?.eventType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ currentEvent?.resourceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ currentEvent?.resourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ formatAction(currentEvent?.action) }}</el-descriptions-item>
        <el-descriptions-item label="请求ID">{{ currentEvent?.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源IP">{{ currentEvent?.sourceIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时(ms)">{{ currentEvent?.costMs ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag
            :type="currentEvent?.result === 1 ? 'success' : 'danger'"
            effect="dark"
            style="border: none"
          >
            {{ currentEvent?.result === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发生时间">
          {{ formatDateTime(currentEvent?.occurredAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          {{ currentEvent?.errorMessage || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listIdentityAuditEvents } from '@/api/identity'
import type { IdentityAuditEvent } from '@/types/entity'

const loading = ref(false)
const tableData = ref<IdentityAuditEvent[]>([])

const detailDialogVisible = ref(false)
const currentEvent = ref<IdentityAuditEvent | null>(null)

const searchForm = reactive({
  operatorKeyword: '',
  result: undefined as number | undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listIdentityAuditEvents({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      operatorKeyword: searchForm.operatorKeyword || undefined,
      result: searchForm.result
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取审计事件失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.operatorKeyword = ''
  searchForm.result = undefined
  pagination.pageNum = 1
  fetchData()
}

const handleViewDetail = (row: IdentityAuditEvent) => {
  currentEvent.value = row
  detailDialogVisible.value = true
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

const ACTION_TEXT_MAP: Record<string, string> = {
  login: '登录',
  logout: '登出',
  create: '创建',
  update: '更新',
  revoke: '撤销',
  bindUser: '绑定用户',
  grantRoles: '分配角色',
  grantPermissions: '分配权限',
  resetPassword: '重置密码'
}

const formatAction = (action?: string) => {
  if (!action) return '-'
  return ACTION_TEXT_MAP[action] || action
}

const formatOperator = (event?: IdentityAuditEvent | null) => {
  if (!event) return '-'
  const operatorId = event.operatorId
  const operatorName = event.operatorName
  if (operatorName && operatorId != null) {
    return `${operatorName}@${operatorId}`
  }
  if (operatorName) {
    return operatorName
  }
  if (operatorId != null) {
    return String(operatorId)
  }
  return '-'
}

onMounted(() => {
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
</style>
