<template>
  <div class="audit-log">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="Trace ID">
          <el-input
            v-model="searchForm.traceId"
            placeholder="请输入 Trace ID"
            clearable
            style="width: 300px"
          />
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
        <el-table-column prop="traceId" label="Trace ID" width="200" />
        <el-table-column prop="modelId" label="模型ID" width="100" />
        <el-table-column prop="taskType" label="任务类型" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Token 使用" width="150">
          <template #default="{ row }">
            <div>输入: {{ row.promptTokens }}</div>
            <div>输出: {{ row.completionTokens }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="requestTime" label="请求时间" width="180" />
        <el-table-column prop="responseTime" label="响应时间" width="180" />
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
      title="调用日志详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Trace ID">
          {{ currentLog?.traceId }}
        </el-descriptions-item>
        <el-descriptions-item label="模型ID">
          {{ currentLog?.modelId }}
        </el-descriptions-item>
        <el-descriptions-item label="任务类型">
          {{ currentLog?.taskType }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog?.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ currentLog?.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求时间">
          {{ currentLog?.requestTime }}
        </el-descriptions-item>
        <el-descriptions-item label="响应时间">
          {{ currentLog?.responseTime }}
        </el-descriptions-item>
        <el-descriptions-item label="输入 Tokens">
          {{ currentLog?.promptTokens }}
        </el-descriptions-item>
        <el-descriptions-item label="输出 Tokens">
          {{ currentLog?.completionTokens }}
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog?.errorMessage">
          <el-text type="danger">{{ currentLog?.errorMessage }}</el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogList } from '@/api/audit'
import type { CallLog } from '@/types/entity'

const loading = ref(false)
const tableData = ref<CallLog[]>([])
const dialogVisible = ref(false)
const currentLog = ref<CallLog | null>(null)

const searchForm = reactive({
  traceId: ''
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
      traceId: searchForm.traceId || undefined
    })
    tableData.value = res.data.data.list
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
  searchForm.traceId = ''
  pagination.pageNum = 1
  fetchData()
}

// 查看详情
const handleViewDetail = (row: CallLog) => {
  currentLog.value = row
  dialogVisible.value = true
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
  fetchData()
})
</script>

<style scoped>
.audit-log {
  width: 100%;
}

.search-form {
  margin-bottom: 20px;
}
</style>
