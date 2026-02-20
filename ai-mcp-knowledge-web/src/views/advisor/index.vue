<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Advisor 管理</h2>
        <p class="subtitle">可配置的 ChatClient Advisors</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建 Advisor
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input v-model="q.keyword" placeholder="按 code/name/type 搜索" style="width: 260px" @keyup.enter="fetchData" />
          <el-select v-model="q.advisorType" clearable placeholder="类型" style="width: 200px; margin-left: 10px">
            <el-option label="CHAT_MEMORY" value="CHAT_MEMORY" />
            <el-option label="REQUEST_RESPONSE_LOG" value="REQUEST_RESPONSE_LOG" />
            <el-option label="TOOL_CALL_LOG" value="TOOL_CALL_LOG" />
          </el-select>
          <el-select v-model="q.enabled" clearable placeholder="启用状态" style="width: 160px; margin-left: 10px">
            <el-option label="启用" :value="true" />
            <el-option label="禁用" :value="false" />
          </el-select>
          <el-button class="gemini-btn-secondary" style="margin-left: 10px" @click="fetchData">查询</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
            <el-icon><Plus /></el-icon>
            新建
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="advisorCode" label="code" min-width="160" />
        <el-table-column prop="advisorName" label="名称" min-width="160" />
        <el-table-column prop="advisorType" label="类型" min-width="200" />
        <el-table-column label="启用" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? 'ENABLED' : 'DISABLED' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button v-if="row.enabled === 0" link type="success" class="action-btn" @click="enable(row)">
                <el-icon><CircleCheckFilled /></el-icon>
              </el-button>
              <el-button v-else link type="warning" class="action-btn" @click="disable(row)">
                <el-icon><CircleCloseFilled /></el-icon>
              </el-button>
              <el-button link type="danger" class="action-btn" @click="removeRow(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 18px 0">
            <el-empty description="暂无 Advisor">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreate">新建 Advisor</el-button>
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

    <el-dialog v-model="dlg.visible" :title="dlg.title" width="860px" class="gemini-dialog">
      <el-form :model="dlg.form" label-width="140px" class="gemini-form">
        <el-form-item label="advisorCode">
          <el-input v-model="dlg.form.advisorCode" placeholder="例如 chat_memory" />
        </el-form-item>
        <el-form-item label="advisorName">
          <el-input v-model="dlg.form.advisorName" placeholder="例如 对话记忆" />
        </el-form-item>
        <el-form-item label="advisorType">
          <el-select v-model="dlg.form.advisorType" style="width: 100%">
            <el-option label="CHAT_MEMORY" value="CHAT_MEMORY" />
            <el-option label="REQUEST_RESPONSE_LOG" value="REQUEST_RESPONSE_LOG" />
            <el-option label="TOOL_CALL_LOG" value="TOOL_CALL_LOG" />
          </el-select>
          <div class="form-hint" v-if="dlg.form.advisorType === 'CHAT_MEMORY'">
            configJson 示例：<span class="mono">{ "maxMessages": 20, "conversationIdFrom": "SESSION_ID", "prefix": "s:" }</span>
          </div>
        </el-form-item>
        <el-form-item label="enabled">
          <el-switch v-model="dlg.form.enabled" />
        </el-form-item>
        <el-form-item label="configJson">
          <el-input v-model="dlg.form.configJson" type="textarea" :rows="8" placeholder="可空；必须为合法 JSON 字符串" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="dlg.saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { disableAdvisor, enableAdvisor, listAdvisors, removeAdvisor, saveAdvisor, type Advisor } from '@/api/advisor'
import { formatDateTime } from '@/utils/time'
import { CircleCheckFilled, CircleCloseFilled, Delete, EditPen, Plus, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref<Advisor[]>([])

const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const q = reactive<{ keyword?: string; enabled?: boolean | null; advisorType?: string | null }>({
  keyword: '',
  enabled: null,
  advisorType: null
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAdvisors({
      keyword: q.keyword || undefined,
      enabled: q.enabled === null ? undefined : (q.enabled as any),
      advisorType: q.advisorType || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (s: number) => {
  pageSize.value = s
  pageNum.value = 1
  fetchData()
}

const handlePageChange = (p: number) => {
  pageNum.value = p
  fetchData()
}

const dlg = reactive({
  visible: false,
  saving: false,
  title: '新建 Advisor',
  form: {
    id: undefined as number | undefined,
    advisorCode: '',
    advisorName: '',
    advisorType: 'CHAT_MEMORY',
    enabled: true,
    configJson: ''
  }
})

const openCreate = () => {
  dlg.title = '新建 Advisor'
  dlg.form.id = undefined
  dlg.form.advisorCode = ''
  dlg.form.advisorName = ''
  dlg.form.advisorType = 'CHAT_MEMORY'
  dlg.form.enabled = true
  dlg.form.configJson = ''
  dlg.visible = true
}

const openEdit = (row: Advisor) => {
  dlg.title = `编辑 Advisor: ${row.advisorCode}`
  dlg.form.id = row.id
  dlg.form.advisorCode = row.advisorCode
  dlg.form.advisorName = row.advisorName
  dlg.form.advisorType = row.advisorType
  dlg.form.enabled = row.enabled === 1
  dlg.form.configJson = row.configJson || ''
  dlg.visible = true
}

const save = async () => {
  if (!dlg.form.advisorCode || !dlg.form.advisorName || !dlg.form.advisorType) {
    ElMessage.error('请填写 advisorCode/advisorName/advisorType')
    return
  }
  dlg.saving = true
  try {
    await saveAdvisor({
      id: dlg.form.id,
      advisorCode: dlg.form.advisorCode,
      advisorName: dlg.form.advisorName,
      advisorType: dlg.form.advisorType,
      enabled: dlg.form.enabled,
      configJson: dlg.form.configJson || undefined
    })
    ElMessage.success('保存成功')
    dlg.visible = false
    fetchData()
  } finally {
    dlg.saving = false
  }
}

const enable = async (row: Advisor) => {
  await enableAdvisor(row.id)
  ElMessage.success('已启用')
  fetchData()
}

const disable = async (row: Advisor) => {
  await disableAdvisor(row.id)
  ElMessage.success('已禁用')
  fetchData()
}

const removeRow = async (row: Advisor) => {
  await ElMessageBox.confirm(`确认删除 Advisor ${row.advisorCode}？`, '提示', { type: 'warning' })
  await removeAdvisor(row.id)
  ElMessage.success('已删除')
  fetchData()
}

fetchData()
</script>
