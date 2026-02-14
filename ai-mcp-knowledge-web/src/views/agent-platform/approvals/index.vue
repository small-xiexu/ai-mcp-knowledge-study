<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">工具审批</h2>
        <p class="subtitle">HIGH 风险工具默认生成审批单，通过后方式B自动续跑</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <div class="table-toolbar">
        <el-select v-model="status" placeholder="状态" clearable style="width: 180px">
          <el-option label="PENDING" value="PENDING" />
          <el-option label="APPROVED" value="APPROVED" />
          <el-option label="REJECTED" value="REJECTED" />
          <el-option label="EXPIRED" value="EXPIRED" />
        </el-select>
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.status === 'PENDING' ? 'warning' : (row.status === 'APPROVED' ? 'success' : 'info')">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="toolKey" label="toolKey" min-width="220" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="risk" width="120" />
        <el-table-column prop="runId" label="runId" min-width="240" show-overflow-tooltip />
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.expireAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openDetail(row)">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button
                link
                type="success"
                class="action-btn"
                :disabled="row.status !== 'PENDING'"
                @click="approve(row)"
              >
                <el-icon><CircleCheckFilled /></el-icon>
              </el-button>
              <el-button
                link
                type="danger"
                class="action-btn"
                :disabled="row.status !== 'PENDING'"
                @click="reject(row)"
              >
                <el-icon><CircleCloseFilled /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="审批单详情" width="860px" class="gemini-dialog">
      <pre class="detail-pre">{{ JSON.stringify(currentDetail, null, 2) }}</pre>
      <template #footer>
        <el-button class="gemini-btn-primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="续跑结果（Platform Contract v1）" width="900px" class="gemini-dialog">
      <pre class="detail-pre">{{ JSON.stringify(currentResult, null, 2) }}</pre>
      <template #footer>
        <el-button class="gemini-btn-primary" @click="resultVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveTool, getApproval, listApprovals, rejectTool, type PlatformContractV1 } from '@/api/agent-platform'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const tableData = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const status = ref<string | undefined>()

const detailVisible = ref(false)
const currentDetail = ref<any>(null)

const resultVisible = ref(false)
const currentResult = ref<PlatformContractV1 | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listApprovals({ status: status.value, pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openDetail = async (row: any) => {
  const res = await getApproval(row.id)
  currentDetail.value = res.data
  detailVisible.value = true
}

const approve = async (row: any) => {
  const { value } = await ElMessageBox.prompt('审批意见（可空）', '审批通过', {
    confirmButtonText: '通过并续跑',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：同意执行'
  })
  const res = await approveTool({ id: row.id, decisionComment: value || undefined })
  currentResult.value = res.data
  resultVisible.value = true
  ElMessage.success('已通过并续跑完成')
  fetchData()
}

const reject = async (row: any) => {
  const { value } = await ElMessageBox.prompt('拒绝原因（可空）', '审批拒绝', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：风险过高'
  })
  await rejectTool({ id: row.id, decisionComment: value || undefined })
  ElMessage.success('已拒绝')
  fetchData()
}

const handlePageChange = (p: number) => {
  pageNum.value = p
  fetchData()
}

const handleSizeChange = (s: number) => {
  pageSize.value = s
  pageNum.value = 1
  fetchData()
}

fetchData()
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.detail-pre {
  max-height: 560px;
  overflow: auto;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}
</style>

