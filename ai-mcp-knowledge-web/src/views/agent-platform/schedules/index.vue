<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Agent 调度</h2>
        <p class="subtitle">调度执行时始终取当前发布版本（current_published_version_id）</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建调度
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input v-model="agentCode" placeholder="过滤 agentCode" clearable style="width: 260px" @keyup.enter="fetchData" />
          <el-input v-model="scheduleName" placeholder="过滤调度名称" clearable style="width: 220px" @keyup.enter="fetchData" />
          <el-select v-model="enabled" placeholder="启用状态" clearable style="width: 160px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
          <el-button class="gemini-btn-secondary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button class="gemini-btn-secondary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="agentCode" label="agentCode" min-width="160" />
        <el-table-column prop="scheduleName" label="调度名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="cron" label="CRON" min-width="180" />
        <el-table-column prop="xxlJobId" label="xxlJobId" width="120" />
        <el-table-column prop="enabled" label="启用" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openPayload(row)">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button link type="primary" class="action-btn" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button
                link
                class="action-btn"
                :type="row.enabled ? 'warning' : 'success'"
                @click="toggle(row)"
              >
                <el-icon v-if="row.enabled"><VideoPause /></el-icon>
                <el-icon v-else><VideoPlay /></el-icon>
              </el-button>
              <el-button link type="danger" class="action-btn" @click="remove(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 18px 0">
            <el-empty description="暂无调度">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreate">新建调度</el-button>
            </el-empty>
          </div>
        </template>
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
    </el-card>

    <el-dialog v-model="editVisible" :title="editTitle" width="860px" class="gemini-dialog">
      <el-form :model="form" label-width="120px" class="gemini-form">
        <el-form-item label="agentCode">
          <el-input v-model="form.agentCode" :disabled="isEdit" placeholder="目标 agentCode" />
        </el-form-item>
        <el-form-item label="调度名称">
          <el-input v-model="form.scheduleName" placeholder="同一个 Agent 下唯一" />
        </el-form-item>
        <el-form-item label="调度描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，便于识别不同计划任务" />
        </el-form-item>
        <el-form-item label="cron">
          <el-input v-model="form.cron" placeholder="例如：0 */5 * * * ?" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="payloadTemplateJson">
          <el-input v-model="form.payloadTemplateJson" type="textarea" :rows="6" placeholder='例如：{"content":"xxx","ragTagsJson":"[\"tag1\"]"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payloadVisible" title="payloadTemplateJson" width="900px" class="gemini-dialog">
      <pre class="payload-pre">{{ payloadText || '-' }}</pre>
      <template #footer>
        <el-button class="gemini-btn-primary" @click="payloadVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSchedule,
  disableSchedule,
  enableSchedule,
  listSchedules,
  removeSchedule,
  updateSchedule,
  type AgentSchedule
} from '@/api/agent-platform'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<AgentSchedule[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const agentCode = ref('')
const scheduleName = ref('')
const enabled = ref<boolean | undefined>()

const editVisible = ref(false)
const isEdit = ref(false)
const editTitle = computed(() => (isEdit.value ? '编辑调度' : '新建调度'))
const payloadVisible = ref(false)
const payloadText = ref('')

const form = reactive<any>({
  id: undefined,
  agentCode: '',
  scheduleName: '',
  description: '',
  cron: '',
  enabled: true,
  payloadTemplateJson: '{"content":"请输出今日待办","ragTagsJson":"[]"}'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listSchedules({
      agentCode: agentCode.value || undefined,
      scheduleName: scheduleName.value || undefined,
      enabled: enabled.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  form.id = undefined
  form.agentCode = ''
  form.scheduleName = ''
  form.description = ''
  form.cron = ''
  form.enabled = true
  form.payloadTemplateJson = '{"content":"请输出今日待办","ragTagsJson":"[]"}'
  editVisible.value = true
}

const openEdit = (row: AgentSchedule) => {
  isEdit.value = true
  form.id = row.id
  form.agentCode = row.agentCode || ''
  form.scheduleName = row.scheduleName || ''
  form.description = row.description || ''
  form.cron = row.cron
  form.enabled = Boolean(row.enabled)
  form.payloadTemplateJson = row.payloadTemplateJson || ''
  editVisible.value = true
}

const openPayload = (row: AgentSchedule) => {
  payloadText.value = row.payloadTemplateJson || ''
  payloadVisible.value = true
}

const save = async () => {
  if (!form.agentCode || !form.scheduleName || !form.cron) {
    ElMessage.error('agentCode/scheduleName/cron 不能为空')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateSchedule({
        id: form.id,
        agentCode: form.agentCode,
        scheduleName: form.scheduleName,
        description: form.description || undefined,
        cron: form.cron,
        payloadTemplateJson: form.payloadTemplateJson || undefined
      })
    } else {
      await createSchedule({
        agentCode: form.agentCode,
        scheduleName: form.scheduleName,
        description: form.description || undefined,
        cron: form.cron,
        enabled: form.enabled,
        payloadTemplateJson: form.payloadTemplateJson || undefined
      })
    }
    editVisible.value = false
    ElMessage.success('保存成功')
    fetchData()
  } finally {
    saving.value = false
  }
}

const toggle = async (row: AgentSchedule) => {
  if (row.enabled) {
    await disableSchedule(row.id)
    ElMessage.success('已禁用')
  } else {
    await enableSchedule(row.id)
    ElMessage.success('已启用')
  }
  fetchData()
}

const remove = async (row: AgentSchedule) => {
  await ElMessageBox.confirm(`确认删除调度 id=${row.id}？将同时尝试删除 xxl-job。`, '删除确认', { type: 'warning' })
  await removeSchedule(row.id)
  ElMessage.success('已删除')
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
.payload-pre {
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
