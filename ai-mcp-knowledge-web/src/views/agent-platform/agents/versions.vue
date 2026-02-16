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

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button class="gemini-btn-secondary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" class="gemini-btn-primary" @click="openCreateDraft">
            <el-icon><Plus /></el-icon>
            新建草稿
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="versionNo" label="版本号" width="110" />
        <el-table-column prop="state" label="状态" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.state === 'PUBLISHED' ? 'success' : (row.state === 'DRAFT' ? 'warning' : 'info')">
              {{ row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeSummary" label="变更摘要" min-width="220" />
        <el-table-column prop="promptTemplateId" label="模板ID" width="110" />
        <el-table-column prop="workflowVersionId" label="WorkflowVersion" width="140">
          <template #default="{ row }">
            <span v-if="row.workflowVersionId" class="mono">#{{ row.workflowVersionId }}</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ragMode" label="RAG" width="120" />
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="right">
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

        <template #empty>
          <div style="padding: 18px 0">
            <el-empty description="暂无版本">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreateDraft">新建草稿</el-button>
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
      <el-form :model="form" label-width="130px" class="gemini-form">
        <el-form-item label="变更摘要">
          <el-input v-model="form.changeSummary" placeholder="建议填写，方便审计与回溯" />
        </el-form-item>

        <el-form-item label="运行模式">
          <el-radio-group v-model="runMode">
            <el-radio-button label="PROMPT">Prompt 模板</el-radio-button>
            <el-radio-button label="WORKFLOW">Workflow</el-radio-button>
          </el-radio-group>
          <div class="form-hint">
            选择 <span class="mono">Workflow</span> 后，Agent 调用会转发到绑定的 WorkflowVersion 执行，并返回每步 steps 明细。
          </div>
        </el-form-item>

        <template v-if="runMode === 'PROMPT'">
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
        </template>
        <template v-else>
          <el-form-item label="绑定 Workflow">
            <el-select v-model="selectedWorkflowId" filterable clearable placeholder="选择 Workflow" style="width: 100%" @change="onWorkflowChange">
              <el-option
                v-for="w in workflowOptions"
                :key="w.id"
                :label="`${w.workflowName} (${w.workflowCode})`"
                :value="w.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="workflowVersionId">
            <el-select v-model="form.workflowVersionId" filterable clearable placeholder="选择 Workflow 版本" style="width: 100%">
              <el-option
                v-for="v in workflowVersionOptions"
                :key="v.id"
                :label="`v${v.versionNo} (${v.state}) - ${v.id}`"
                :value="v.id"
              />
            </el-select>
          </el-form-item>
        </template>

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

        <el-divider content-position="left">Advisors</el-divider>
        <el-form-item label="Advisor 绑定">
          <div style="width: 100%">
            <div v-if="runMode === 'WORKFLOW'" class="muted">
              当前为 <span class="mono">Workflow</span> 运行模式：请在对应 <span class="mono">WorkflowVersion</span> 上绑定 Advisors（此处不生效）。
            </div>
            <template v-else>
              <div class="bind-row">
                <el-select v-model="advisorPickerId" filterable clearable placeholder="选择 Advisor" style="width: 100%">
                  <el-option
                    v-for="a in advisorOptions"
                    :key="a.id"
                    :label="`${a.advisorCode} | ${a.advisorType} | ${a.advisorName}`"
                    :value="a.id"
                  />
                </el-select>
                <el-button class="gemini-btn-secondary" style="margin-left: 10px" @click="addAdvisorBinding">添加</el-button>
              </div>

              <div class="bind-list" v-if="boundAdvisors.length">
                <div v-for="(it, idx) in boundAdvisors" :key="it.advisorId" class="bind-item">
                  <div class="bind-left">
                    <div class="bind-title">{{ advisorLabel(it.advisorId) }}</div>
                    <div class="bind-sub muted">order={{ idx }}</div>
                  </div>
                  <div class="bind-right">
                    <el-switch v-model="it.enabled" active-text="启用" inactive-text="禁用" />
                    <el-button class="gemini-btn-secondary" @click="moveAdvisorUp(idx)">上移</el-button>
                    <el-button class="gemini-btn-secondary" @click="moveAdvisorDown(idx)">下移</el-button>
                    <el-button class="gemini-btn-danger" @click="removeAdvisorBinding(idx)">移除</el-button>
                  </div>
                </div>
              </div>
              <div v-else class="muted">未绑定 Advisor（默认仅注入全局 TraceIdAdvisor）。</div>
            </template>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="editVisible = false">取消</el-button>
        <el-button v-if="form.id" class="gemini-btn-secondary" :loading="preheating" @click="preheatDraft">预热</el-button>
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
import { listAdvisorBindings, listAdvisors, saveAdvisorBindings, type Advisor } from '@/api/advisor'
import { preheatAgentVersion } from '@/api/preheat'
import { listMcpTools } from '@/api/mcp'
import { listRagTags } from '@/api/rag'
import { getWorkflowVersion, listWorkflows, listWorkflowVersions, type Workflow, type WorkflowVersion } from '@/api/workflow'
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
const workflowOptions = ref<Workflow[]>([])
const workflowVersionOptions = ref<WorkflowVersion[]>([])
const selectedWorkflowId = ref<number | undefined>(undefined)
const advisorOptions = ref<Advisor[]>([])
const advisorPickerId = ref<number | undefined>(undefined)
const boundAdvisors = ref<Array<{ advisorId: number; enabled: boolean }>>([])
const preheating = ref(false)

const editVisible = ref(false)
const isEdit = ref(false)
const editTitle = computed(() => (isEdit.value ? '编辑草稿' : '新建草稿'))

const snapshotVisible = ref(false)
const currentSnapshot = ref('')

const defaultRagTags = ref<string[]>([])
const allowedRagTags = ref<string[]>([])
const allowedToolKeys = ref<string[]>([])
const runMode = ref<'PROMPT' | 'WORKFLOW'>('PROMPT')

const form = reactive<any>({
  id: undefined,
  changeSummary: '',
  promptTemplateId: undefined,
  templateParamsJson: '{}',
  workflowVersionId: undefined,
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
  try {
    const wfs = await listWorkflows({ offset: 0, pageSize: 200 })
    workflowOptions.value = wfs.data.records || []
  } catch {
    workflowOptions.value = []
  }
  try {
    const res = await listAdvisors({ pageNum: 1, pageSize: 200, enabled: true })
    advisorOptions.value = res.data?.records || []
  } catch {
    advisorOptions.value = []
  }
}

const openCreateDraft = async () => {
  isEdit.value = false
  form.id = undefined
  form.changeSummary = ''
  form.promptTemplateId = undefined
  form.templateParamsJson = '{}'
  form.workflowVersionId = undefined
  runMode.value = 'PROMPT'
  selectedWorkflowId.value = undefined
  workflowVersionOptions.value = []
  form.ragMode = 'OPTIONAL'
  form.repairRetryTimes = 2
  form.timeoutMs = 60000
  form.maxTurns = 20
  form.temperature = 0.7
  defaultRagTags.value = []
  allowedRagTags.value = []
  allowedToolKeys.value = []
  boundAdvisors.value = []
  advisorPickerId.value = undefined
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
    form.workflowVersionId = v.workflowVersionId
    runMode.value = v.workflowVersionId ? 'WORKFLOW' : 'PROMPT'
    form.ragMode = v.ragMode || 'OPTIONAL'
    form.repairRetryTimes = v.repairRetryTimes ?? 2
    form.timeoutMs = v.timeoutMs ?? 60000
    form.maxTurns = v.maxTurns ?? 20
    form.temperature = v.temperature ?? 0.7
    defaultRagTags.value = safeParseList(v.defaultRagTagsJson)
    allowedRagTags.value = safeParseList(v.allowedRagTagsJson)
    allowedToolKeys.value = safeParseList(v.allowedToolKeysJson)
    await loadOptions()

    // workflowVersionId -> 回填 workflow 选择
    selectedWorkflowId.value = undefined
    workflowVersionOptions.value = []
    if (v.workflowVersionId) {
      try {
        const wv = await getWorkflowVersion(v.workflowVersionId)
        selectedWorkflowId.value = wv.data.workflowId
        const versions = await listWorkflowVersions({ workflowId: wv.data.workflowId })
        workflowVersionOptions.value = (versions.data || []).slice().sort((a, b) => (b.versionNo || 0) - (a.versionNo || 0))
      } catch {}
    }
    advisorPickerId.value = undefined
    if (runMode.value === 'PROMPT' && form.id) {
      await loadBindings(Number(form.id))
    } else {
      boundAdvisors.value = []
    }
    editVisible.value = true
  } finally {
    saving.value = false
  }
}

const onWorkflowChange = async () => {
  workflowVersionOptions.value = []
  form.workflowVersionId = undefined
  if (!selectedWorkflowId.value) {
    return
  }
  const versions = await listWorkflowVersions({ workflowId: selectedWorkflowId.value })
  workflowVersionOptions.value = (versions.data || []).slice().sort((a, b) => (b.versionNo || 0) - (a.versionNo || 0))
}

const loadBindings = async (agentVersionId: number) => {
  try {
    const res = await listAdvisorBindings({ bindType: 'AGENT_VERSION', bindTargetId: agentVersionId })
    const list = res.data || []
    boundAdvisors.value = list
      .slice()
      .sort((a, b) => (a.orderNo || 0) - (b.orderNo || 0))
      .map(v => ({ advisorId: v.advisorId, enabled: v.bindingEnabled === 1 }))
  } catch {
    boundAdvisors.value = []
  }
}

const advisorLabel = (advisorId: number) => {
  const a = advisorOptions.value.find(x => x.id === advisorId)
  if (!a) return `#${advisorId}`
  return `${a.advisorCode} | ${a.advisorType} | ${a.advisorName}`
}

const addAdvisorBinding = () => {
  if (!advisorPickerId.value) {
    ElMessage.warning('请选择 Advisor')
    return
  }
  const exists = boundAdvisors.value.some(x => x.advisorId === advisorPickerId.value)
  if (exists) {
    ElMessage.warning('已绑定该 Advisor')
    return
  }
  boundAdvisors.value.push({ advisorId: advisorPickerId.value, enabled: true })
  advisorPickerId.value = undefined
}

const removeAdvisorBinding = (idx: number) => {
  boundAdvisors.value.splice(idx, 1)
}

const moveAdvisorUp = (idx: number) => {
  if (idx <= 0) return
  const arr = boundAdvisors.value
  const tmp = arr[idx - 1]
  arr[idx - 1] = arr[idx]
  arr[idx] = tmp
}

const moveAdvisorDown = (idx: number) => {
  const arr = boundAdvisors.value
  if (idx < 0 || idx >= arr.length - 1) return
  const tmp = arr[idx + 1]
  arr[idx + 1] = arr[idx]
  arr[idx] = tmp
}

const preheatDraft = async () => {
  if (!form.id) return
  preheating.value = true
  try {
    const res = await preheatAgentVersion({ agentVersionId: Number(form.id), refreshMcp: false })
    const warnings = res.data?.warnings || []
    if (warnings.length) {
      ElMessage.warning(`预热完成（有告警 ${warnings.length} 条）`)
    } else {
      ElMessage.success('预热完成')
    }
  } finally {
    preheating.value = false
  }
}

const saveDraft = async () => {
  if (runMode.value === 'WORKFLOW') {
    if (!form.workflowVersionId) {
      ElMessage.error('请选择 workflowVersionId')
      return
    }
    // 绑定 workflow 时，promptTemplate 允许为空；这里避免误配：主动清空
    form.promptTemplateId = undefined
  } else {
    form.workflowVersionId = undefined
  }

  saving.value = true
  try {
    const saved = await saveAgentVersionDraft({
      id: form.id,
      agentCode,
      changeSummary: form.changeSummary || undefined,
      promptTemplateId: form.promptTemplateId || undefined,
      templateParamsJson: form.templateParamsJson || undefined,
      workflowVersionId: form.workflowVersionId || undefined,
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
    const savedId = saved.data?.id
    if (runMode.value === 'PROMPT' && savedId) {
      await saveAdvisorBindings({
        bindType: 'AGENT_VERSION',
        bindTargetId: Number(savedId),
        items: (boundAdvisors.value || []).map((it, idx) => ({
          advisorId: it.advisorId,
          orderNo: idx,
          enabled: it.enabled
        }))
      })
    }
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
.muted {
  color: var(--gemini-text-secondary);
}
.form-hint {
  margin-left: 12px;
  color: var(--gemini-text-secondary);
  font-size: 12px;
  line-height: 1.4;
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
.bind-row {
  display: flex;
  align-items: center;
}
.bind-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bind-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}
.bind-title {
  font-weight: 600;
}
.bind-sub {
  font-size: 12px;
}
.bind-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
