<template>
  <div class="gemini-container">
    <div class="page-header">
      <h2 class="page-title">网关工具管理台</h2>
      <el-button type="primary" class="gemini-btn-primary" @click="openEdit()">新增网关实例</el-button>
    </div>

    <el-alert
      title="用途说明"
      type="warning"
      description="本页用于管理网关实例，维护每个网关的基础信息、状态和工具集合入口。"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-card class="gemini-card">
      <el-table :data="records" class="gemini-table" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="gatewayId" label="网关 ID" min-width="140" />
        <el-table-column prop="gatewayName" label="名称" min-width="180" />
        <el-table-column prop="gatewayVersion" label="版本" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="toolCount" label="工具数" width="90" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="goTools(row)">工具配置</el-button>
            <el-button type="primary" text @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" text @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :total="page.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="网关实例" width="680px" class="gemini-dialog">
      <el-form :model="form" label-width="110px">
        <el-form-item label="网关 ID">
          <el-text type="info">{{ DEFAULT_GATEWAY_ID }}（系统固定）</el-text>
        </el-form-item>
        <el-form-item label="网关名称">
          <el-input v-model="form.gatewayName" />
        </el-form-item>
        <el-form-item label="网关版本">
          <el-input v-model="form.gatewayVersion" placeholder="1.0.0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
        <el-form-item label="网关描述">
          <el-input v-model="form.gatewayDesc" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="网关说明">
          <el-input v-model="form.gatewayInstructions" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listGatewayInstances, saveGatewayInstance, deleteGatewayInstance } from '@/api/gateway'
import type { GatewayInstance } from '@/types/gateway'

const DEFAULT_GATEWAY_ID = 'default_gateway'
const router = useRouter()
const loading = ref(false)
const records = ref<GatewayInstance[]>([])
const editVisible = ref(false)

const page = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive<GatewayInstance>({
  id: undefined,
  gatewayId: DEFAULT_GATEWAY_ID,
  gatewayName: '',
  gatewayVersion: '1.0.0',
  gatewayDesc: '',
  gatewayInstructions: '',
  status: 1
})

const enabledSwitch = computed({
  get: () => form.status === 1,
  set: (val: boolean) => {
    form.status = val ? 1 : 0
  }
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listGatewayInstances({ pageNum: page.pageNum, pageSize: page.pageSize })
    records.value = res.data.records || []
    page.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const openEdit = (row?: GatewayInstance) => {
  if (!row) {
    Object.assign(form, {
      id: undefined,
      gatewayId: DEFAULT_GATEWAY_ID,
      gatewayName: '',
      gatewayVersion: '1.0.0',
      gatewayDesc: '',
      gatewayInstructions: '',
      status: 1
    })
  } else {
    Object.assign(form, {
      ...row,
      gatewayId: row.gatewayId || DEFAULT_GATEWAY_ID
    })
  }
  editVisible.value = true
}

const saveRow = async () => {
  try {
    if (!form.gatewayId) {
      form.gatewayId = DEFAULT_GATEWAY_ID
    }
    await saveGatewayInstance(form)
    ElMessage.success('保存成功')
    editVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const removeRow = async (row: GatewayInstance) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除网关 ${row.gatewayName}？`, '提示', { type: 'warning' })
    await deleteGatewayInstance(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // ignore
  }
}

const goTools = (row: GatewayInstance) => {
  router.push(`/gateway-tools/${row.gatewayId || DEFAULT_GATEWAY_ID}/tools`)
}

onMounted(fetchData)
</script>
