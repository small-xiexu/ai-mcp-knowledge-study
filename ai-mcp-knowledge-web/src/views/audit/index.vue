<template>
  <div class="audit-log">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="表名">
          <el-select
            v-model="searchForm.tableName"
            placeholder="请选择表名"
            clearable
            filterable
            style="width: 260px"
          >
            <el-option
              v-for="name in tableOptions"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tableName" label="表名" width="180" />
        <el-table-column prop="recordId" label="记录ID" width="120" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-tag :type="getOperationTagType(row.operation)">
              {{ row.operation }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="oldValue" label="变更前" min-width="200" show-overflow-tooltip />
        <el-table-column prop="newValue" label="变更后" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="审计详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="表名">
          {{ currentLog?.tableName }}
        </el-descriptions-item>
        <el-descriptions-item label="记录ID">
          {{ currentLog?.recordId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作">
          <el-tag :type="getOperationTagType(currentLog?.operation)">
            {{ currentLog?.operation }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentLog?.operator }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ currentLog?.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item label="变更前" :span="2">
          <el-text>{{ currentLog?.oldValue || '-' }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="变更后" :span="2">
          <el-text>{{ currentLog?.newValue || '-' }}</el-text>
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

// 获取审计日志列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAuditLogList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      tableName: searchForm.tableName || undefined
    })
    tableData.value = res.data.data.records
    pagination.total = res.data.data.total
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
    tableOptions.value = res.data.data || []
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
</style>
