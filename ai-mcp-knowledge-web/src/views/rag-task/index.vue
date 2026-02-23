<template>
  <div class="gemini-container rag-task-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">导入任务</h2>
        <p class="subtitle">查看知识库异步导入任务进度，失败任务可直接重试。</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button class="gemini-btn-primary" @click="goKnowledge">
          <el-icon><Files /></el-icon>
          去知识库管理
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        class="gemini-table"
        style="width: 100%"
      >
        <el-table-column
          prop="taskId"
          label="任务ID"
          min-width="240"
        >
          <template #default="{ row }">
            <span class="task-id">{{ row.taskId }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="type"
          label="类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag size="small" effect="dark">{{ row.type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="ragTag"
          label="知识库标签"
          min-width="160"
        >
          <template #default="{ row }">
            {{ row.ragTag || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="130"
          align="center"
          header-align="center"
        >
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="statusTagType(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="更新时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="进度" width="220" align="center" header-align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress || 0"
              :status="progressStatus(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="重试次数"
          width="120"
        >
          <template #default="{ row }">
            {{ row.retryCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column
          label="错误详情"
          width="120"
        >
          <template #default="{ row }">
            <el-popover
              v-if="row.errorDetails"
              placement="left"
              width="460"
              trigger="hover"
            >
              <template #reference>
                <el-tag
                  type="danger"
                  size="small"
                  class="error-tag"
                >
                  查看错误
                </el-tag>
              </template>
              <div class="error-pre-wrapper">
                <pre class="error-pre">{{ formatErrorDetails(row.errorDetails) }}</pre>
              </div>
            </el-popover>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="message"
          label="状态说明"
          min-width="200"
        />
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
          align="center"
          header-align="center"
        >
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

        <template #empty>
          <div class="table-empty">
            <el-empty description="暂无导入任务">
              <el-button type="primary" class="gemini-btn-primary" @click="goKnowledge">
                去导入文档
              </el-button>
            </el-empty>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          background
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Files, Refresh } from '@element-plus/icons-vue'
import { cancelRagTask, listRagTasks, retryTask } from '@/api/rag'
import type { RagTask } from '@/types/entity'

const router = useRouter()
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

const goKnowledge = () => {
  router.push('/knowledge')
}

const formatStatus = (status?: string) => {
  if (!status) return '-'
  const map: Record<string, string> = {
    PENDING: '排队中',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败',
    PARTIAL_SUCCESS: '部分成功',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const statusTagType = (status?: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PARTIAL_SUCCESS') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return ''
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
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
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
    const newTaskId = res.data
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}

.error-tag {
  cursor: pointer;
}

.error-pre-wrapper {
  max-height: 360px;
  overflow-y: auto;
}

.error-pre {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  margin: 0;
}

.table-empty {
  padding: 18px 0;
}
</style>
