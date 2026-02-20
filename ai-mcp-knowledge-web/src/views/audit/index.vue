<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <!-- 搜索栏 -->
      <el-form
        :inline="true"
        :model="searchForm"
        class="search-form"
      >
        <el-form-item label="表名">
          <el-select
            v-model="searchForm.tableName"
            placeholder="请选择表名"
            clearable
            filterable
            style="width: 260px"
            class="gemini-select"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              v-for="name in tableOptions"
              :key="name"
              :label="formatTableOption(name)"
              :value="name"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="gemini-btn-primary"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button
            class="gemini-btn-secondary"
            @click="handleReset"
          >
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        class="gemini-table"
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column label="表名" width="220">
          <template #default="{ row }">
            <span>{{ getTableLabel(row.tableName) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="recordId"
          label="记录ID"
          width="120"
        />
        <el-table-column
          label="操作"
          width="120"
        >
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operation)" effect="dark" style="border: none;">
              {{ getOperationLabel(row.operation) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="operator"
          label="操作人"
          width="120"
        />
        <el-table-column
          prop="oldValue"
          label="变更前"
          min-width="200"

        />
        <el-table-column
          prop="newValue"
          label="变更后"
          min-width="200"

        />
        <el-table-column
          label="操作时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="120"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              text
              class="action-btn"
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="审计详情"
      width="800px"
      class="gemini-dialog"
      align-center
    >
      <el-descriptions
        class="gemini-descriptions"
        :column="2"
        border
      >
        <el-descriptions-item label="表名">
          {{ getTableLabel(currentLog?.tableName) }}
        </el-descriptions-item>
        <el-descriptions-item label="记录ID">
          {{ currentLog?.recordId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作">
          <el-tag
            :type="getOperationTagType(currentLog?.operation)"
            effect="dark"
            style="border: none;"
          >
            {{ getOperationLabel(currentLog?.operation) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentLog?.operator }}
        </el-descriptions-item>
        <el-descriptions-item
          label="操作时间"
          :span="2"
        >
          {{ formatDateTime(currentLog?.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item
          label="变更前"
          :span="2"
        >
          <pre class="json-code">{{ formatJson(currentLog?.oldValue) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item
          label="变更后"
          :span="2"
        >
          <pre class="json-code">{{ formatJson(currentLog?.newValue) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogList, getAuditTableNames } from '@/api/audit'
import type { ConfigAudit } from '@/types/entity'

const loading = ref(false)
const tableData = ref<ConfigAudit[]>([])
const dialogVisible = ref(false)
const currentLog = ref<ConfigAudit | null>(null)
const tableOptions = ref<string[]>([])

const searchForm = reactive({
  tableName: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableNameLabelMap: Record<string, string> = {
  ai_model_config: '模型配置（ai_model_config）',
  ai_mcp_server_config: 'MCP 服务配置（ai_mcp_server_config）',
  mcp_gateway: 'Gateway 实例（mcp_gateway）',
  mcp_tool_registry: 'Gateway 工具（mcp_tool_registry）',
  mcp_tool_binding: '工具绑定（mcp_tool_binding）',
  mcp_tool_mapping: '参数映射（mcp_tool_mapping）',
  mcp_tool_schema: 'Schema 缓存（mcp_tool_schema）',
  mcp_gateway_auth: '网关鉴权（mcp_gateway_auth）'
}

const operationLabelMap: Record<string, string> = {
  INSERT: '新增',
  UPDATE: '更新',
  DELETE: '删除',
  CREATE: '创建',
  ENABLE: '启用',
  DISABLE: '禁用',
  DEBUG: '测试',
  BIND: '绑定',
  GATEWAY_INSTANCE_CREATE: '网关实例-创建',
  GATEWAY_INSTANCE_UPDATE: '网关实例-更新',
  GATEWAY_INSTANCE_DELETE: '网关实例-删除',
  GATEWAY_TOOL_CREATE: '网关工具-创建',
  GATEWAY_TOOL_UPDATE: '网关工具-更新',
  GATEWAY_TOOL_DELETE: '网关工具-删除',
  GATEWAY_TOOL_ENABLE: '网关工具-启用',
  GATEWAY_TOOL_DISABLE: '网关工具-禁用',
  GATEWAY_AUTH_CREATE: '网关凭证-创建',
  GATEWAY_AUTH_UPDATE: '网关凭证-更新',
  GATEWAY_AUTH_ENABLE: '网关凭证-启用',
  GATEWAY_AUTH_DISABLE: '网关凭证-禁用',
  GATEWAY_MODEL_BINDING_UPDATE: '网关模型绑定-更新'
}

// 获取审计日志列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAuditLogList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      tableName: searchForm.tableName || undefined
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '获取审计日志失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.tableName = ''
  pagination.pageNum = 1
  fetchData()
}

// 查看详情
const handleViewDetail = (row: ConfigAudit) => {
  currentLog.value = row
  dialogVisible.value = true
}

const getOperationTagType = (operation?: string) => {
  if (!operation) return 'info'
  const normalized = operation.toUpperCase()
  if (normalized.includes('ENABLE')) {
    return 'success'
  }
  if (normalized.includes('DISABLE')) {
    return 'info'
  }
  if (normalized.includes('BIND')) {
    return 'primary'
  }
  if (normalized.includes('CREATE') || normalized.includes('INSERT')) {
    return 'success'
  }
  if (normalized.includes('UPDATE')) {
    return 'warning'
  }
  if (normalized.includes('DELETE')) {
    return 'danger'
  }
  return 'info'
}

const getOperationLabel = (operation?: string) => {
  if (!operation) return '-'
  const normalized = operation.toUpperCase()
  return operationLabelMap[normalized] || operation
}

const getTableLabel = (tableName?: string) => {
  if (!tableName) return '-'
  return tableNameLabelMap[tableName] || tableName
}

const formatTableOption = (tableName?: string) => {
  if (!tableName) return '-'
  return getTableLabel(tableName)
}

const formatJson = (jsonStr?: string) => {
  if (!jsonStr) return '-'
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return jsonStr
  }
}

const formatDateTime = (val?: string) => {
  if (!val) return '-'
  // Handle 2026-02-03T10:32:33 or 2026-02-03T10:32:33.123
  return val.replace('T', ' ').substring(0, 19)
}

// 分页大小变化
const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

// 页码变化
const handleCurrentChange = () => {
  fetchData()
}

onMounted(() => {
  fetchTableOptions()
  fetchData()
})

const fetchTableOptions = async () => {
  try {
    const res = await getAuditTableNames()
    tableOptions.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取表名列表失败')
  }
}
</script>

<style scoped>
.audit-log {
  width: 100%;
}

.search-form {
  margin-bottom: 20px;
}

.gemini-descriptions {
  --el-descriptions-table-border: 1px solid var(--gemini-border);
  --el-descriptions-item-bordered-label-background: rgba(255, 255, 255, 0.05);
}

:deep(.gemini-descriptions .el-descriptions__label) {
  color: var(--gemini-text-secondary);
  font-weight: 500;
  width: 120px;
}

:deep(.gemini-descriptions .el-descriptions__content) {
  color: var(--gemini-text-primary);
  background-color: transparent !important;
}

.json-code {
  margin: 0;
  padding: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  font-family: monospace;
  font-size: 13px;
  color: var(--gemini-accent);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}
</style>
