<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Agent 版本</h2>
        <p class="subtitle">agentCode: <span class="mono">{{ agentCode }}</span></p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreateDraft">
          <el-icon><Plus /></el-icon>
          新建草稿
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="versionNo" label="versionNo" width="110" />
        <el-table-column prop="state" label="state" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.state === 'PUBLISHED' ? 'success' : (row.state === 'DRAFT' ? 'warning' : 'info')">
              {{ row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeSummary" label="变更摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="promptTemplateId" label="模板ID" width="110" />
        <el-table-column prop="ragMode" label="RAG" width="120" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openSnapshot(row)">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button
                v-if="row.state === 'DRAFT'"
                link
                type="primary"
                class="action-btn"
                @click="openEditDraft(row)"
              >
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button
                v-if="row.state === 'DRAFT'"
                link
                type="success"
                class="action-btn"
                @click="publish(row)"
              >
                <el-icon><CircleCheckFilled /></el-icon>
              </el-button>
              <el-button
                v-if="row.state === 'PUBLISHED'"
                link
                type="warning"
                class="action-btn"
                @click="rollback(row)"
              >
                <el-icon><RefreshRight /></el-icon>
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

    <el-dialog v-model="editVisible" :title="editTitle" width="860px" class="gemini-dialog">
      <el-form :model="form" label-width="130px" class="gemini-form">
        <el-form-item label="变更摘要">
          <el-input v-model="form.changeSummary" placeholder="建议填写，方便审计与回溯" />
        </el-form-item>
        <el-form-item label="Prompt 模板">
          <el-select v-model="form.promptTemplateId" filterable clearable placeholder="选择模板" style="width: 100%">
            <el-option
              v-for="t in templateOptions"
              :key="t.id"
              :label="`${t.templateName} (${t.scope}:${t.templateCode})`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模板参数(JSON)">
          <el-input v-model="form.templateParamsJson" type="textarea" :rows="3" placeholder='例如：{"name":"xxx"}' />
        </el-form-item>
        <el-form-item label="RAG 模式">
          <el-select v-model="form.ragMode" style="width: 100%">
            <el-option label="DISABLED" value="DISABLED" />
            <el-option label="OPTIONAL" value="OPTIONAL" />
            <el-option label="REQUIRED" value="REQUIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认 RAG tags">
          <el-select v-model="defaultRagTags" multiple filterable clearable style="width: 100%">
            <el-option v-for="t in ragTagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许 RAG tags">
          <el-select v-model="allowedRagTags" multiple filterable clearable style="width: 100%">
            <el-option v-for="t in ragTagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许工具(toolKey)">
          <el-select v-model="allowedToolKeys" multiple filterable clearable style="width: 100%">
            <el-option v-for="t in toolOptions" :key="t.toolKey" :label="`${t.toolKey} | ${t.name}`" :value="t.toolKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="修复重试次数">
          <el-input-number v-model="form.repairRetryTimes" :min="0" :max="5" />
        </el-form-item>
        <el-form-item label="超时(ms)">
          <el-input-number v-model="form.timeoutMs" :min="1000" :max="600000" />
        </el-form-item>
        <el-form-item label="maxTurns">
          <el-input-number v-model="form.maxTurns" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="saveDraft">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="snapshotVisible" title="system_prompt_snapshot" width="900px" class="gemini-dialog">
      <pre class="snapshot-pre">{{ currentSnapshot || '-' }}</pre>
      <template #footer>
        <el-button class="gemini-btn-primary" @click="snapshotVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAgentVersion,
  listAgentVersions,
  listTemplates,
  publishAgentVersion,
  rollbackAgentVersion,
  saveAgentVersionDraft,
  type AgentVersion,
  type PromptTemplate
} from '@/api/agent-platform'
import { listMcpTools } from '@/api/mcp'
import { listRagTags } from '@/api/rag'
import { formatDateTime } from '@/utils/time'

const route = useRoute()
const agentCode = String(route.params.agentCode || '')

const loading = ref(false)
const saving = ref(false)
const tableData = ref<AgentVersion[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const templateOptions = ref<PromptTemplate[]>([])
const ragTagOptions = ref<string[]>([])
const toolOptions = ref<Array<{ name: string; toolKey: string }>>([])

const editVisible = ref(false)
const isEdit = ref(false)
const editTitle = computed(() => (isEdit.value ? '编辑草稿' : '新建草稿'))

const snapshotVisible = ref(false)
const currentSnapshot = ref('')

const defaultRagTags = ref<string[]>([])
const allowedRagTags = ref<string[]>([])
const allowedToolKeys = ref<string[]>([])

const form = reactive<any>({
  id: undefined,
  changeSummary: '',
  promptTemplateId: undefined,
  templateParamsJson: '{}',
  ragMode: 'OPTIONAL',
  repairRetryTimes: 2,
  timeoutMs: 60000,
  maxTurns: 20,
  temperature: 0.7
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAgentVersions({ agentCode, pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const templates = await listTemplates({ pageNum: 1, pageSize: 200 })
    templateOptions.value = templates.data.records || []
  } catch {}
  try {
    const tags = await listRagTags()
    ragTagOptions.value = tags.data || []
  } catch {}
  try {
    const tools = await listMcpTools()
    toolOptions.value = (tools.data || []).map((t: any) => ({ name: t.name, toolKey: t.toolKey }))
  } catch {}
}

const openCreateDraft = async () => {
  isEdit.value = false
  form.id = undefined
  form.changeSummary = ''
  form.promptTemplateId = undefined
  form.templateParamsJson = '{}'
  form.ragMode = 'OPTIONAL'
  form.repairRetryTimes = 2
  form.timeoutMs = 60000
  form.maxTurns = 20
  form.temperature = 0.7
  defaultRagTags.value = []
  allowedRagTags.value = []
  allowedToolKeys.value = []
  await loadOptions()
  editVisible.value = true
}

const openEditDraft = async (row: AgentVersion) => {
  isEdit.value = true
  saving.value = true
  try {
    const res = await getAgentVersion(row.id)
    const v = res.data
    form.id = v.id
    form.changeSummary = v.changeSummary || ''
    form.promptTemplateId = v.promptTemplateId
    form.templateParamsJson = v.templateParamsJson || '{}'
    form.ragMode = v.ragMode || 'OPTIONAL'
    form.repairRetryTimes = v.repairRetryTimes ?? 2
    form.timeoutMs = v.timeoutMs ?? 60000
    form.maxTurns = v.maxTurns ?? 20
    form.temperature = v.temperature ?? 0.7
    defaultRagTags.value = safeParseList(v.defaultRagTagsJson)
    allowedRagTags.value = safeParseList(v.allowedRagTagsJson)
    allowedToolKeys.value = safeParseList(v.allowedToolKeysJson)
    await loadOptions()
    editVisible.value = true
  } finally {
    saving.value = false
  }
}

const saveDraft = async () => {
  saving.value = true
  try {
    await saveAgentVersionDraft({
      id: form.id,
      agentCode,
      changeSummary: form.changeSummary || undefined,
      promptTemplateId: form.promptTemplateId || undefined,
      templateParamsJson: form.templateParamsJson || undefined,
      ragMode: form.ragMode,
      defaultRagTagsJson: JSON.stringify(defaultRagTags.value || []),
      allowedRagTagsJson: JSON.stringify(allowedRagTags.value || []),
      allowedToolKeysJson: JSON.stringify(allowedToolKeys.value || []),
      outputContractVersion: 'v1',
      timeoutMs: form.timeoutMs,
      maxTurns: form.maxTurns,
      temperature: form.temperature,
      repairRetryTimes: form.repairRetryTimes
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    await fetchData()
  } finally {
    saving.value = false
  }
}

const publish = async (row: AgentVersion) => {
  await ElMessageBox.confirm(`确认发布 versionNo=${row.versionNo}？发布会固化模板快照，并切换为当前生效版本。`, '发布确认', {
    type: 'warning'
  })
  await publishAgentVersion({ agentCode, versionId: row.id })
  ElMessage.success('发布成功')
  fetchData()
}

const rollback = async (row: AgentVersion) => {
  await ElMessageBox.confirm(`确认回滚到 versionNo=${row.versionNo}？（下一次调用/调度将使用该发布版本）`, '回滚确认', {
    type: 'warning'
  })
  await rollbackAgentVersion({ agentCode, targetVersionId: row.id })
  ElMessage.success('回滚成功')
  fetchData()
}

const openSnapshot = (row: AgentVersion) => {
  currentSnapshot.value = row.systemPromptSnapshot || ''
  snapshotVisible.value = true
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

const safeParseList = (json?: string) => {
  if (!json) return []
  try {
    const v = JSON.parse(json)
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

fetchData()
</script>

<style scoped lang="scss">
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  color: var(--gemini-accent);
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
.snapshot-pre {
  max-height: 520px;
  overflow: auto;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}
</style>

