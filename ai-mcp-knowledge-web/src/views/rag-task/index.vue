<template>
  <div class="rag-task-page">
    <el-card>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="taskId" label="任务ID" min-width="220" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="ragTag" label="知识库标签" min-width="160" />
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="progressStatus(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="重试次数" width="100">
          <template #default="{ row }">
            {{ row.retryCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="错误详情" width="120">
          <template #default="{ row }">
            <el-popover v-if="row.errorDetails" placement="left" width="500" trigger="hover">
              <template #reference>
                <el-tag type="danger" size="small" style="cursor: pointer">查看错误</el-tag>
              </template>
              <div style="max-height: 400px; overflow-y: auto">
                <pre style="white-space: pre-wrap; word-wrap: break-word; font-size: 12px">{{ formatErrorDetails(row.errorDetails) }}</pre>
              </div>
            </el-popover>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="状态说明" min-width="200" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button
              size="small"
              type="warning"
              :disabled="row.status !== 'PROCESSING' && row.status !== 'PENDING'"
              @click="handleCancel(row.taskId)"
            >
              取消
            </el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="row.status !== 'FAILED' && row.status !== 'PARTIAL_SUCCESS'"
              @click="handleRetry(row.taskId)"
            >
              重试
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelRagTask, listRagTasks, retryTask } from '@/api/rag'
import type { RagTask } from '@/types/entity'

const loading = ref(false)
const tableData = ref<RagTask[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

const progressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'CANCELLED') return 'warning'
  return ''
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listRagTasks({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.data.records || []
    pagination.total = res.data.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = async (taskId: string) => {
  try {
    await cancelRagTask(taskId)
    ElMessage.success('取消成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '取消失败')
  }
}

const handleRetry = async (taskId: string) => {
  try {
    const res = await retryTask(taskId)
    const newTaskId = res.data.data
    ElMessage.success(`重试任务已创建: ${newTaskId}`)
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '重试失败')
  }
}

const formatErrorDetails = (errorDetails: string) => {
  try {
    const errors = JSON.parse(errorDetails)
    if (Array.isArray(errors)) {
      return errors.map((err, index) =>
        `[${index + 1}] 文件: ${err.fileName}\n` +
        `    错误: ${err.errorMessage}\n` +
        `    时间: ${err.occurredAt}\n` +
        `    重试: ${err.retryCount || 0} 次\n`
      ).join('\n')
    }
    return errorDetails
  } catch {
    return errorDetails
  }
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.rag-task-page {
  width: 100%;
}
</style>
