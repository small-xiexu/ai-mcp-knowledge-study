<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Workflow 版本</h2>
        <p class="subtitle">{{ wfLabel }}</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreateVersion">
          <el-icon><Plus /></el-icon>
          新建版本
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <el-table :data="versions" stripe class="gemini-table" v-loading="loading">
        <el-table-column prop="versionNo" label="版本号" width="120" />
        <el-table-column prop="state" label="状态" width="150">
          <template #default="{ row }">
            <el-tag size="small" :type="row.state === 'PUBLISHED' ? 'success' : 'info'">{{ row.state }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeSummary" label="变更说明" min-width="220" />
        <el-table-column prop="id" label="versionId" width="140" />
        <el-table-column label="操作" min-width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">画布编辑</el-button>
            <el-button link type="primary" @click="goPlayground(row)">调用</el-button>
            <el-button v-if="row.state !== 'PUBLISHED'" link type="success" @click="publish(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg.visible" title="新建版本" width="520px">
      <el-form :model="dlg.form" label-width="120px">
        <el-form-item label="changeSummary">
          <el-input v-model="dlg.form.changeSummary" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="createVersion">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { createWorkflowVersion, getWorkflow, listWorkflowVersions, publishWorkflowVersion, type Workflow, type WorkflowVersion } from '@/api/workflow'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const wf = ref<Workflow | null>(null)
const versions = ref<WorkflowVersion[]>([])

const workflowId = computed(() => {
  const raw = route.query.workflowId
  return raw ? Number(raw) : 0
})
const workflowCode = computed(() => String(route.params.workflowCode || ''))

const wfLabel = computed(() => {
  if (wf.value) {
    return `${wf.value.workflowName} (${wf.value.workflowCode})`
  }
  return workflowCode.value || '-'
})

const load = async () => {
  if (!workflowId.value) {
    ElMessage.error('缺少 workflowId（请从列表页进入）')
    return
  }
  loading.value = true
  try {
    const w = await getWorkflow(workflowId.value)
    wf.value = w.data
    const res = await listWorkflowVersions({ workflowId: workflowId.value })
    versions.value = (res.data || []).slice().sort((a, b) => (b.versionNo || 0) - (a.versionNo || 0))
  } finally {
    loading.value = false
  }
}

const dlg = reactive({
  visible: false,
  saving: false,
  form: {
    changeSummary: ''
  }
})

const openCreateVersion = () => {
  dlg.form.changeSummary = ''
  dlg.visible = true
}

const createVersion = async () => {
  if (!workflowId.value) return
  dlg.saving = true
  try {
    await createWorkflowVersion({ workflowId: workflowId.value, changeSummary: dlg.form.changeSummary || undefined })
    ElMessage.success('创建成功')
    dlg.visible = false
    load()
  } finally {
    dlg.saving = false
  }
}

const publish = async (v: WorkflowVersion) => {
  await publishWorkflowVersion({ workflowVersionId: v.id })
  ElMessage.success('发布成功')
  load()
}

const openEditor = (v: WorkflowVersion) => {
  router.push({
    name: 'WorkflowEditor',
    params: { workflowCode: workflowCode.value },
    query: { workflowId: String(workflowId.value), workflowVersionId: String(v.id) }
  })
}

const goPlayground = (v: WorkflowVersion) => {
  router.push({
    name: 'WorkflowPlayground',
    query: { workflowCode: workflowCode.value, workflowVersionId: String(v.id) }
  })
}

load()
</script>

