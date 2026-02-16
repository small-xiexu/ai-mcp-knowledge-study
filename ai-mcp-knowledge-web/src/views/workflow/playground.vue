<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Workflow 调用</h2>
        <p class="subtitle">按 workflowCode 运行（返回 Platform Contract v1，并包含 steps 明细）</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="loadWorkflows">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <el-form :model="form" label-width="140px" class="gemini-form">
        <el-form-item label="workflowCode">
          <el-select v-model="form.workflowCode" filterable clearable placeholder="选择 Workflow" style="width: 100%" @change="onWorkflowChange">
            <el-option v-for="w in workflowOptions" :key="w.workflowCode" :label="`${w.workflowName} (${w.workflowCode})`" :value="w.workflowCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="workflowVersionId">
          <el-select v-model="form.workflowVersionId" filterable clearable placeholder="可选：指定版本" style="width: 100%">
            <el-option v-for="v in versionOptions" :key="v.id" :label="`v${v.versionNo} (${v.state}) - ${v.id}`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="sessionId">
          <el-input v-model="form.sessionId" placeholder="可选" />
        </el-form-item>
        <el-form-item label="variablesJson">
          <el-input v-model="form.variablesJson" type="textarea" :rows="3" placeholder='例如：{"foo":"bar"}' />
        </el-form-item>
        <el-form-item label="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="输入内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="gemini-btn-primary" :loading="running" @click="run">
            <el-icon><Lightning /></el-icon>
            执行
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="gemini-card" style="margin-top: 14px">
      <div class="result-title">结果</div>
      <div v-if="resultObj" class="pretty-box">
        <ContractViewer :contract="resultObj" />
      </div>
      <el-empty v-else description="暂无结果" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import ContractViewer from '@/components/contract/ContractViewer.vue'
import type { PlatformContractV1 } from '@/types/workflow'
import { listWorkflows, listWorkflowVersions, runWorkflow, type Workflow, type WorkflowVersion } from '@/api/workflow'

const route = useRoute()

const workflowOptions = ref<Workflow[]>([])
const versionOptions = ref<WorkflowVersion[]>([])

const running = ref(false)
const resultObj = ref<PlatformContractV1 | null>(null)

const form = reactive({
  workflowCode: '',
  workflowVersionId: undefined as number | undefined,
  sessionId: '',
  variablesJson: '',
  content: ''
})

const workflowIdByCode = computed(() => {
  const w = workflowOptions.value.find(x => x.workflowCode === form.workflowCode)
  return w?.id || 0
})

const loadWorkflows = async () => {
  const res = await listWorkflows({ offset: 0, pageSize: 200 })
  workflowOptions.value = res.data.records || []

  const presetCode = String(route.query.workflowCode || '')
  const presetVersionId = route.query.workflowVersionId ? Number(route.query.workflowVersionId) : undefined
  if (presetCode && !form.workflowCode) {
    form.workflowCode = presetCode
  }
  if (presetVersionId && !form.workflowVersionId) {
    form.workflowVersionId = presetVersionId
  }
  if (form.workflowCode) {
    await onWorkflowChange()
  }
}

const onWorkflowChange = async () => {
  if (!workflowIdByCode.value) {
    versionOptions.value = []
    return
  }
  const res = await listWorkflowVersions({ workflowId: workflowIdByCode.value })
  versionOptions.value = (res.data || []).slice().sort((a, b) => (b.versionNo || 0) - (a.versionNo || 0))
}

const run = async () => {
  if (!form.workflowCode) {
    ElMessage.error('请选择 workflowCode')
    return
  }
  if (!form.content) {
    ElMessage.error('content 不能为空')
    return
  }
  if (form.variablesJson && String(form.variablesJson).trim()) {
    try {
      JSON.parse(String(form.variablesJson))
    } catch {
      ElMessage.error('variablesJson 不是合法 JSON')
      return
    }
  }
  const sessionId = form.sessionId && String(form.sessionId).trim() ? Number(form.sessionId) : undefined
  if (form.sessionId && Number.isNaN(sessionId as any)) {
    ElMessage.error('sessionId 必须是数字')
    return
  }

  running.value = true
  try {
    const res = await runWorkflow(form.workflowCode, {
      sessionId,
      content: form.content,
      variablesJson: form.variablesJson || undefined,
      workflowVersionId: form.workflowVersionId
    })
    resultObj.value = res.data as PlatformContractV1
    if (resultObj.value?.status === 'PENDING_APPROVAL') {
      ElMessage.warning(`需要审批：approvalRequestId=${resultObj.value.meta?.approvalRequestId}`)
    }
    if (resultObj.value?.status === 'FAILED') {
      ElMessage.error('执行失败（详情见 error 与 steps）')
    }
  } finally {
    running.value = false
  }
}

loadWorkflows()
</script>

<style scoped lang="scss">
.result-title {
  color: var(--gemini-text-primary);
  font-weight: 600;
  margin-bottom: 10px;
}
.pretty-box {
  max-height: 720px;
  overflow: auto;
}
</style>

