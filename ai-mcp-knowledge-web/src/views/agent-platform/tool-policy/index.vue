<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">工具策略</h2>
        <p class="subtitle">按 org + toolKey 配置风险等级与审批门禁（HIGH 默认生成审批单）</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建策略
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="模糊匹配 toolKey" clearable style="width: 320px" @keyup.enter="fetchData" />
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
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="toolKey" label="toolKey" min-width="320" />
        <el-table-column prop="riskLevel" label="risk" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.riskLevel === 'HIGH' ? 'danger' : (row.riskLevel === 'LOW' ? 'success' : 'warning')">
              {{ row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approvalRequired" label="审批" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.approvalRequired === 1 ? 'danger' : 'info'">
              {{ row.approvalRequired === 1 ? '需要' : '不需要' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button
                link
                class="action-btn"
                :type="row.enabled === 1 ? 'warning' : 'success'"
                @click="toggle(row)"
              >
                <el-icon v-if="row.enabled === 1"><VideoPause /></el-icon>
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
            <el-empty description="暂无工具策略">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreate">新建策略</el-button>
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
      <el-form :model="form" label-width="140px" class="gemini-form">
        <el-form-item label="toolKey">
          <el-select
            v-model="form.toolKey"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入 toolKey"
            style="width: 100%"
            :disabled="isEdit"
          >
            <el-option
              v-for="t in toolOptions"
              :key="t.toolKey"
              :label="`${t.toolKey} (${t.source})`"
              :value="t.toolKey"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="riskLevel">
          <el-select v-model="form.riskLevel" placeholder="风险等级" style="width: 220px">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="approvalRequired">
          <el-switch v-model="form.approvalRequired" />
        </el-form-item>
        <el-form-item label="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注（可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { disableToolPolicy, enableToolPolicy, listToolPolicies, removeToolPolicy, saveToolPolicy, type ToolPolicy } from '@/api/agent-platform'
import { listMcpTools } from '@/api/mcp'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<ToolPolicy[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const keyword = ref('')
const enabled = ref<boolean | undefined>()

const toolOptions = ref<Array<{ toolKey: string; source?: string; description?: string }>>([])

const editVisible = ref(false)
const isEdit = ref(false)
const editTitle = computed(() => (isEdit.value ? '编辑工具策略' : '新建工具策略'))

const form = reactive<any>({
  id: undefined,
  toolKey: '',
  riskLevel: 'MEDIUM',
  approvalRequired: false,
  enabled: true,
  remark: ''
})

const fetchTools = async () => {
  try {
    const res = await listMcpTools()
    toolOptions.value = (res.data || []).map((x: any) => ({
      toolKey: x.toolKey,
      source: x.source,
      description: x.description
    }))
  } catch {
    toolOptions.value = []
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listToolPolicies({
      keyword: keyword.value || undefined,
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

const openCreate = async () => {
  isEdit.value = false
  form.id = undefined
  form.toolKey = ''
  form.riskLevel = 'MEDIUM'
  form.approvalRequired = false
  form.enabled = true
  form.remark = ''
  editVisible.value = true
  if (!toolOptions.value.length) {
    fetchTools()
  }
}

const openEdit = async (row: ToolPolicy) => {
  isEdit.value = true
  form.id = row.id
  form.toolKey = row.toolKey
  form.riskLevel = row.riskLevel || 'MEDIUM'
  form.approvalRequired = row.approvalRequired === 1
  form.enabled = row.enabled === 1
  form.remark = row.remark || ''
  editVisible.value = true
  if (!toolOptions.value.length) {
    fetchTools()
  }
}

const save = async () => {
  if (!form.toolKey) {
    ElMessage.error('toolKey 不能为空')
    return
  }
  saving.value = true
  try {
    await saveToolPolicy({
      id: form.id,
      toolKey: form.toolKey,
      riskLevel: form.riskLevel,
      approvalRequired: Boolean(form.approvalRequired),
      enabled: Boolean(form.enabled),
      remark: form.remark || undefined
    })
    editVisible.value = false
    ElMessage.success('保存成功')
    fetchData()
  } finally {
    saving.value = false
  }
}

const toggle = async (row: ToolPolicy) => {
  if (row.enabled === 1) {
    await disableToolPolicy(row.id)
    ElMessage.success('已禁用')
  } else {
    await enableToolPolicy(row.id)
    ElMessage.success('已启用')
  }
  fetchData()
}

const remove = async (row: ToolPolicy) => {
  await ElMessageBox.confirm(`确认删除该策略？\\n${row.toolKey}`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await removeToolPolicy(row.id)
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
fetchTools()
</script>

<style scoped lang="scss"></style>
