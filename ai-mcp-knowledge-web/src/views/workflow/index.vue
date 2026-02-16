<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Workflow 管理</h2>
        <p class="subtitle">独立 Workflow 资产，支持版本、画布编辑、运行与运行明细</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建 Workflow
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="搜索 code/name" style="max-width: 360px" @keyup.enter="load" />
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table :data="rows" stripe class="gemini-table" v-loading="loading">
        <el-table-column prop="workflowCode" label="Code" min-width="180" />
        <el-table-column prop="workflowName" label="名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentPublishedVersionId" label="已发布版本" width="140" />
        <el-table-column label="操作" min-width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goVersions(row)">版本</el-button>
            <el-button link type="primary" @click="goEditor(row)">画布</el-button>
            <el-button link type="primary" @click="goPlayground(row)">调用</el-button>
            <el-button link @click="openUpdate(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="prev, pager, next, sizes, total"
          :current-page="page.pageNum"
          :page-size="page.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="page.total"
          @update:current-page="onPageNum"
          @update:page-size="onPageSize"
        />
      </div>
    </div>

    <el-dialog v-model="dlg.visible" :title="dlg.mode === 'create' ? '新建 Workflow' : '编辑 Workflow'" width="520px">
      <el-form :model="dlg.form" label-width="120px">
        <el-form-item label="workflowCode" v-if="dlg.mode === 'create'">
          <el-input v-model="dlg.form.workflowCode" placeholder="例如: order_flow" />
        </el-form-item>
        <el-form-item label="workflowName">
          <el-input v-model="dlg.form.workflowName" placeholder="名称" />
        </el-form-item>
        <el-form-item label="description">
          <el-input v-model="dlg.form.description" type="textarea" :rows="3" placeholder="描述" />
        </el-form-item>
        <el-form-item label="status" v-if="dlg.mode === 'update'">
          <el-select v-model="dlg.form.status" style="width: 100%">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="dlg.saving" @click="saveDlg">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createWorkflow, listWorkflows, updateWorkflow, type Workflow } from '@/api/workflow'

const router = useRouter()

const loading = ref(false)
const rows = ref<Workflow[]>([])
const query = reactive({ keyword: '' })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const load = async () => {
  loading.value = true
  try {
    const offset = (Math.max(page.pageNum, 1) - 1) * Math.max(page.pageSize, 1)
    const res = await listWorkflows({ keyword: query.keyword || undefined, offset, pageSize: page.pageSize })
    rows.value = res.data.records || []
    page.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const onPageNum = (v: number) => {
  page.pageNum = v
  load()
}
const onPageSize = (v: number) => {
  page.pageSize = v
  page.pageNum = 1
  load()
}

const dlg = reactive({
  visible: false,
  mode: 'create' as 'create' | 'update',
  saving: false,
  editingId: 0,
  form: {
    workflowCode: '',
    workflowName: '',
    description: '',
    status: 'ENABLED'
  }
})

const openCreate = () => {
  dlg.mode = 'create'
  dlg.editingId = 0
  dlg.form.workflowCode = ''
  dlg.form.workflowName = ''
  dlg.form.description = ''
  dlg.form.status = 'ENABLED'
  dlg.visible = true
}

const openUpdate = (row: Workflow) => {
  dlg.mode = 'update'
  dlg.editingId = row.id
  dlg.form.workflowCode = row.workflowCode
  dlg.form.workflowName = row.workflowName
  dlg.form.description = row.description || ''
  dlg.form.status = row.status || 'ENABLED'
  dlg.visible = true
}

const saveDlg = async () => {
  if (dlg.mode === 'create') {
    if (!dlg.form.workflowCode) {
      ElMessage.error('workflowCode 不能为空')
      return
    }
    if (!dlg.form.workflowName) {
      ElMessage.error('workflowName 不能为空')
      return
    }
  } else {
    if (!dlg.editingId) {
      ElMessage.error('缺少 id')
      return
    }
  }

  dlg.saving = true
  try {
    if (dlg.mode === 'create') {
      await createWorkflow({
        workflowCode: dlg.form.workflowCode.trim(),
        workflowName: dlg.form.workflowName.trim(),
        description: dlg.form.description || undefined
      })
      ElMessage.success('创建成功')
    } else {
      await updateWorkflow({
        id: dlg.editingId,
        workflowName: dlg.form.workflowName.trim(),
        description: dlg.form.description || undefined,
        status: dlg.form.status || undefined
      })
      ElMessage.success('更新成功')
    }
    dlg.visible = false
    load()
  } finally {
    dlg.saving = false
  }
}

const goVersions = (row: Workflow) => {
  router.push({ name: 'WorkflowVersions', params: { workflowCode: row.workflowCode }, query: { workflowId: String(row.id) } })
}

const goEditor = (row: Workflow) => {
  router.push({
    name: 'WorkflowEditor',
    params: { workflowCode: row.workflowCode },
    query: {
      workflowId: String(row.id),
      workflowVersionId: row.currentPublishedVersionId ? String(row.currentPublishedVersionId) : ''
    }
  })
}

const goPlayground = (row: Workflow) => {
  router.push({ name: 'WorkflowPlayground', query: { workflowCode: row.workflowCode } })
}

load()
</script>

<style scoped lang="scss">
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>

