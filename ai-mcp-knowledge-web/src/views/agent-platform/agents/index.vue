<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Agent 管理</h2>
        <p class="subtitle">统一 Agent 控制面</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建 Agent
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索 agentCode / 名称"
            class="gemini-input"
            style="width: 320px"
            clearable
            @keyup.enter="fetchData"
          />
          <el-select v-model="status" placeholder="状态" clearable class="gemini-select" style="width: 160px">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
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
        <el-table-column prop="agentName" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="240" />
        <el-table-column prop="channel" label="channel" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.channel || 'agent' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentPublishedVersionId" label="当前发布版本ID" min-width="140" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button link type="primary" class="action-btn" @click="goVersions(row)">
                <el-icon><List /></el-icon>
              </el-button>
              <el-button link type="danger" class="action-btn" @click="remove(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 18px 0">
            <el-empty description="暂无 Agent">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreate">新建 Agent</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" class="gemini-dialog">
      <el-form :model="form" label-width="110px" class="gemini-form">
        <el-form-item label="agentCode">
          <el-input v-model="form.agentCode" :disabled="isEdit" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.agentName" placeholder="Agent 名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="状态" style="width: 100%">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="channel">
          <el-select v-model="form.channel" placeholder="调用通道" style="width: 100%">
            <el-option label="agent" value="agent" />
            <el-option label="chat_stream" value="chat_stream" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAgent, listAgents, removeAgent, updateAgent, type Agent } from '@/api/agent-platform'
import { formatDateTime } from '@/utils/time'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<Agent[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const status = ref<string | undefined>()

const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑 Agent' : '新建 Agent'))

const form = reactive({
  agentCode: '',
  agentName: '',
  description: '',
  channel: 'agent',
  status: 'ENABLED'
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAgents({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: status.value || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  form.agentCode = ''
  form.agentName = ''
  form.description = ''
  form.channel = 'agent'
  form.status = 'ENABLED'
  dialogVisible.value = true
}

const openEdit = (row: Agent) => {
  isEdit.value = true
  form.agentCode = row.agentCode
  form.agentName = row.agentName
  form.description = row.description || ''
  form.channel = row.channel || 'agent'
  form.status = row.status || 'ENABLED'
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.agentCode || !form.agentName) {
    ElMessage.error('agentCode/名称 不能为空')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateAgent({
        agentCode: form.agentCode,
        agentName: form.agentName,
        description: form.description || undefined,
        channel: form.channel || 'agent',
        status: form.status
      })
    } else {
      await createAgent({
        agentCode: form.agentCode,
        agentName: form.agentName,
        description: form.description || undefined,
        channel: form.channel || 'agent',
        status: form.status
      })
    }
    dialogVisible.value = false
    await fetchData()
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

const goVersions = (row: Agent) => {
  router.push(`/agents/${encodeURIComponent(row.agentCode)}/versions`)
}

const remove = async (row: Agent) => {
  await ElMessageBox.confirm(`确认删除 Agent ${row.agentCode}？会清理关联版本/运行记录/审批数据。`, '删除确认', {
    type: 'warning'
  })
  await removeAgent(row.agentCode)
  ElMessage.success('删除成功')
  await fetchData()
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
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}
</style>
