<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Prompt 模板</h2>
        <p class="subtitle">支持 GLOBAL/ORG 作用域，GLOBAL 仅平台内置可编辑</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建模板
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索编码 / 名称" clearable style="width: 320px" @keyup.enter="fetchData" />
          <el-select v-model="scope" placeholder="作用域" clearable style="width: 160px">
            <el-option label="GLOBAL" value="GLOBAL" />
            <el-option label="ORG" value="ORG" />
          </el-select>
          <el-select v-model="state" placeholder="状态" clearable style="width: 160px">
            <el-option label="DRAFT" value="DRAFT" />
            <el-option label="PUBLISHED" value="PUBLISHED" />
            <el-option label="ARCHIVED" value="ARCHIVED" />
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
        <el-table-column prop="scope" label="作用域" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.scope === 'GLOBAL' ? 'info' : 'success'">
              {{ row.scope }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="templateCode" label="编码" min-width="180" />
        <el-table-column prop="templateName" label="名称" min-width="200" />
        <el-table-column prop="versionNo" label="版本" width="90" />
        <el-table-column prop="state" label="状态" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.state === 'PUBLISHED' ? 'success' : (row.state === 'DRAFT' ? 'warning' : 'info')">
              {{ row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openContent(row)">
                <el-icon><Document /></el-icon>
              </el-button>
              <el-button link type="primary" class="action-btn" :disabled="!canEdit(row)" @click="openEdit(row)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button link type="success" class="action-btn" :disabled="!canPublish(row)" @click="publish(row)">
                <el-icon><CircleCheckFilled /></el-icon>
              </el-button>
              <el-button link type="warning" class="action-btn" :disabled="row.state !== 'PUBLISHED'" @click="archive(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 18px 0">
            <el-empty description="暂无模板">
              <el-button type="primary" class="gemini-btn-primary" @click="openCreate">新建模板</el-button>
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

    <el-dialog v-model="editVisible" :title="editTitle" width="920px" class="gemini-dialog">
      <el-form :model="form" label-width="120px" class="gemini-form">
        <el-form-item label="Scope">
          <el-select v-model="form.scope" :disabled="isEdit" style="width: 100%">
            <el-option label="ORG" value="ORG" />
            <el-option label="GLOBAL" value="GLOBAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="templateCode">
          <el-input v-model="form.templateCode" :disabled="isEdit" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="templateName">
          <el-input v-model="form.templateName" placeholder="名称" />
        </el-form-item>
        <el-form-item label="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="模板正文，支持 {{var}} 占位符" />
        </el-form-item>
        <el-form-item label="variableSpecJson">
          <el-input v-model="form.variableSpecJson" type="textarea" :rows="4" placeholder='变量契约 JSON（可选）' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contentVisible" title="模板内容" width="900px" class="gemini-dialog">
      <pre class="content-pre">{{ currentContent || '-' }}</pre>
      <template #footer>
        <el-button class="gemini-btn-primary" @click="contentVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import {
  archiveTemplate,
  createTemplate,
  listTemplates,
  publishTemplate,
  updateTemplate,
  type PromptTemplate
} from '@/api/agent-platform'
import { formatDateTime } from '@/utils/time'

const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<PromptTemplate[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const scope = ref<string | undefined>()
const state = ref<string | undefined>()

const editVisible = ref(false)
const contentVisible = ref(false)
const isEdit = ref(false)
const editTitle = computed(() => (isEdit.value ? '编辑模板' : '新建模板'))
const currentContent = ref('')

const form = reactive<any>({
  id: undefined,
  scope: 'ORG',
  templateCode: '',
  templateName: '',
  content: '',
  variableSpecJson: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listTemplates({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      scope: scope.value || undefined,
      state: state.value || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const canEdit = (row: PromptTemplate) => {
  if (row.scope === 'GLOBAL') {
    return Boolean(authStore.profile?.superAdmin)
  }
  return true
}

const canPublish = (row: PromptTemplate) => {
  if (!canEdit(row)) return false
  return row.state === 'DRAFT'
}

const openCreate = () => {
  isEdit.value = false
  form.id = undefined
  form.scope = 'ORG'
  form.templateCode = ''
  form.templateName = ''
  form.content = ''
  form.variableSpecJson = ''
  editVisible.value = true
}

const openEdit = (row: PromptTemplate) => {
  isEdit.value = true
  form.id = row.id
  form.scope = row.scope
  form.templateCode = row.templateCode
  form.templateName = row.templateName
  form.content = row.content || ''
  form.variableSpecJson = row.variableSpecJson || ''
  editVisible.value = true
}

const openContent = (row: PromptTemplate) => {
  currentContent.value = row.content || ''
  contentVisible.value = true
}

const save = async () => {
  if (!form.templateCode || !form.templateName || !form.content) {
    ElMessage.error('templateCode/templateName/content 不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      scope: form.scope,
      templateCode: form.templateCode,
      templateName: form.templateName,
      content: form.content,
      variableSpecJson: form.variableSpecJson || undefined
    }
    if (isEdit.value) {
      await updateTemplate(payload)
    } else {
      await createTemplate(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

const publish = async (row: PromptTemplate) => {
  await ElMessageBox.confirm(`确认发布模板 ${row.templateCode}？`, '发布确认', { type: 'warning' })
  await publishTemplate({ id: row.id })
  ElMessage.success('发布成功')
  fetchData()
}

const archive = async (row: PromptTemplate) => {
  await ElMessageBox.confirm(`确认归档模板 ${row.templateCode}？归档后不可再编辑。`, '归档确认', { type: 'warning' })
  await archiveTemplate({ id: row.id })
  ElMessage.success('归档成功')
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
.content-pre {
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
