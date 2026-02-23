<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Client 配置</h2>
        <p class="subtitle">维护可复用的 Client 调用链，Agent 在“链路模式”下可直接引用。</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="fetchData">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新建 Profile
        </el-button>
      </div>
    </div>

    <el-card class="gemini-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索 clientCode / 名称"
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
      </div>

      <el-table v-loading="loading" :data="tableData" class="gemini-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="clientCode" label="clientCode" min-width="160" />
        <el-table-column prop="clientName" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="步骤数" width="100">
          <template #default="{ row }">{{ row.steps?.length || 0 }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" class="action-btn" @click="openEdit(row.id)">
                <el-icon><EditPen /></el-icon>
              </el-button>
              <el-button
                v-if="row.status === 'ENABLED'"
                link
                type="warning"
                class="action-btn"
                @click="toggleStatus(row.id, false)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                link
                type="success"
                class="action-btn"
                @click="toggleStatus(row.id, true)"
              >
                启用
              </el-button>
              <el-button link type="danger" class="action-btn" @click="remove(row.id)">删除</el-button>
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="980px" class="gemini-dialog">
      <el-form :model="form" label-width="120px" class="gemini-form">
        <el-form-item label="clientCode">
          <el-input v-model="form.clientCode" :disabled="isEdit" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.clientName" placeholder="Client 名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="状态" style="width: 100%">
            <el-option label="ENABLED" value="ENABLED" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">步骤链</el-divider>
        <el-form-item label-width="0">
          <div style="width: 100%">
            <el-table :data="form.steps" class="gemini-table" size="small">
              <el-table-column label="序号" width="90">
                <template #default="{ row }">
                  <el-input-number v-model="row.sequenceNo" :min="1" :max="99" />
                </template>
              </el-table-column>
              <el-table-column label="步骤名" min-width="160">
                <template #default="{ row }">
                  <el-input v-model="row.stepName" placeholder="例如：重写问题" />
                </template>
              </el-table-column>
              <el-table-column label="模型" min-width="220">
                <template #default="{ row }">
                  <el-select v-model="row.modelId" filterable placeholder="选择模型" style="width: 100%">
                    <el-option v-for="m in modelOptions" :key="m.id" :label="`${m.modelName} (${m.modelType})`" :value="m.id" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="工具" width="120">
                <template #default="{ row }">
                  <el-switch v-model="row.enableTools" />
                </template>
              </el-table-column>
              <el-table-column label="allowedToolKeysJson" min-width="220">
                <template #default="{ row }">
                  <el-input v-model="row.allowedToolKeysJson" placeholder='例如：["mcp:publish"]' />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" align="center" header-align="center">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeStep($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="margin-top: 10px">
              <el-button class="gemini-btn-secondary" @click="addStep">新增步骤</el-button>
            </div>
          </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelList } from '@/api/model'
import {
  disableClientProfile,
  enableClientProfile,
  getClientProfile,
  listClientProfiles,
  removeClientProfile,
  saveClientProfile,
  type ClientProfile,
  type ClientProfileStep
} from '@/api/client-profile'
import type { ModelConfig } from '@/types/entity'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<ClientProfile[]>([])
const modelOptions = ref<ModelConfig[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const status = ref<string | undefined>()

const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = computed(() => (isEdit.value ? '编辑 Client Profile' : '新建 Client Profile'))

const form = reactive<{
  id?: number
  clientCode: string
  clientName: string
  description: string
  status: string
  steps: ClientProfileStep[]
}>({
  id: undefined,
  clientCode: '',
  clientName: '',
  description: '',
  status: 'ENABLED',
  steps: []
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listClientProfiles({
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

const loadModels = async () => {
  try {
    const res = await getModelList({ pageNum: 1, pageSize: 200 })
    modelOptions.value = res.data.records || []
  } catch {
    modelOptions.value = []
  }
}

const openCreate = async () => {
  isEdit.value = false
  form.id = undefined
  form.clientCode = ''
  form.clientName = ''
  form.description = ''
  form.status = 'ENABLED'
  form.steps = []
  await loadModels()
  dialogVisible.value = true
}

const openEdit = async (id: number) => {
  isEdit.value = true
  await loadModels()
  const res = await getClientProfile(id)
  const p = res.data
  form.id = p.id
  form.clientCode = p.clientCode
  form.clientName = p.clientName
  form.description = p.description || ''
  form.status = p.status || 'ENABLED'
  form.steps = (p.steps || []).map((s, idx) => ({
    id: s.id,
    clientProfileId: s.clientProfileId,
    sequenceNo: s.sequenceNo || idx + 1,
    stepName: s.stepName || '',
    modelId: s.modelId,
    systemPrompt: s.systemPrompt || '',
    enableTools: s.enableTools ?? true,
    allowedToolKeysJson: s.allowedToolKeysJson || ''
  }))
  dialogVisible.value = true
}

const addStep = () => {
  form.steps.push({
    sequenceNo: form.steps.length + 1,
    stepName: '',
    modelId: 0,
    systemPrompt: '',
    enableTools: true,
    allowedToolKeysJson: ''
  })
}

const removeStep = (idx: number) => {
  form.steps.splice(idx, 1)
}

const submit = async () => {
  if (!form.clientCode || !form.clientName) {
    ElMessage.error('clientCode/名称 不能为空')
    return
  }
  if (!form.steps.length) {
    ElMessage.error('至少需要一个步骤')
    return
  }
  if (form.steps.some(s => !s.modelId)) {
    ElMessage.error('步骤模型不能为空')
    return
  }
  saving.value = true
  try {
    await saveClientProfile({
      id: form.id,
      clientCode: form.clientCode,
      clientName: form.clientName,
      description: form.description || undefined,
      status: form.status,
      steps: form.steps.map((s, idx) => ({
        sequenceNo: s.sequenceNo || idx + 1,
        stepName: s.stepName || undefined,
        modelId: s.modelId,
        systemPrompt: s.systemPrompt || undefined,
        enableTools: s.enableTools ?? true,
        allowedToolKeysJson: s.allowedToolKeysJson || undefined
      }))
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchData()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (id: number, enable: boolean) => {
  if (enable) {
    await enableClientProfile(id)
  } else {
    await disableClientProfile(id)
  }
  ElMessage.success(enable ? '已启用' : '已禁用')
  await fetchData()
}

const remove = async (id: number) => {
  await ElMessageBox.confirm('确认删除该 Client Profile？', '删除确认', { type: 'warning' })
  await removeClientProfile(id)
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
.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
