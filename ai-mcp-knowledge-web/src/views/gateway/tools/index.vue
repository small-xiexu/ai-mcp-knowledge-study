<template>
  <div class="gemini-container">
    <div class="page-header">
      <h2 class="page-title">HTTP 工具配置</h2>
      <div>
        <el-button
          v-if="activeTab === 'tools'"
          class="gemini-btn-primary"
          type="primary"
          @click="openCreate"
        >
          新增工具
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="gateway-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="工具配置" name="tools">
        <el-alert
          title="用途说明"
          type="warning"
          description="本页用于配置 HTTP 工具详情，包括参数映射、测试、启停与工具-模型绑定。适合对接公司内部业务接口。"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />

        <el-alert
          title="使用边界提示"
          type="info"
          description="如果你要接入的是标准 MCP Server（STDIO/SSE/HTTP），请前往「MCP 工具配置」页面。"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />

        <el-card class="gemini-card">
          <el-table :data="records" class="gemini-table" style="width: 100%" v-loading="loading">
            <el-table-column prop="toolName" label="工具名称" min-width="180" />
            <el-table-column prop="httpMethod" label="方法" width="90" />
            <el-table-column prop="httpUrl" label="URL" min-width="240" />
            <el-table-column label="状态" width="90" align="center" header-align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="lastCallSummary"
              label="最近调用"
              min-width="170"
              align="center"
              header-align="center"
            />
            <el-table-column label="操作" width="380" fixed="right" align="center" header-align="center">
              <template #default="{ row }">
                <el-button type="primary" text @click="openEdit(row)">编辑</el-button>
                <el-button type="primary" text @click="openDebug(row)">测试</el-button>
                <el-button type="primary" text @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
                <el-button type="danger" text @click="removeTool(row)">删除</el-button>
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

        <el-card class="gemini-card">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>工具-模型绑定</span>
              <el-button type="primary" text @click="bindingVisible = true">配置绑定</el-button>
            </div>
          </template>
          <el-text type="info">未配置绑定时，工具默认对所有模型全局可见。</el-text>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="网关凭证" name="credentials">
        <GatewayCredentialPanel :embedded="true" :default-gateway-id="gatewayId" />
      </el-tab-pane>
    </el-tabs>

    <ToolEditForm
      v-model:visible="editVisible"
      :gateway-id="gatewayId"
      :tool-data="editingPayload"
      @success="fetchData"
    />

    <ToolDebugPanel
      v-model:visible="debugVisible"
      :gateway-id="gatewayId"
      :tool-name="currentToolName"
      :request-mappings="currentToolRequestMappings"
    />

    <ToolBindingDialog v-model:visible="bindingVisible" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listGatewayTools,
  getGatewayTool,
  deleteGatewayTool,
  enableGatewayTool,
  disableGatewayTool
} from '@/api/gateway'
import ToolEditForm from './components/ToolEditForm.vue'
import ToolDebugPanel from './components/ToolDebugPanel.vue'
import ToolBindingDialog from '@/views/gateway/binding/ToolBindingDialog.vue'
import GatewayCredentialPanel from '@/views/gateway/credential/index.vue'
import type { GatewayTool, ParamMappingNode } from '@/types/gateway'

const DEFAULT_GATEWAY_ID = 'default_gateway'
const route = useRoute()
const router = useRouter()
const gatewayId = String(route.params.gatewayId || route.query.gatewayId || DEFAULT_GATEWAY_ID)
const activeTab = ref<'tools' | 'credentials'>('tools')

const loading = ref(false)
const records = ref<GatewayTool[]>([])
const editVisible = ref(false)
const debugVisible = ref(false)
const bindingVisible = ref(false)
const currentToolName = ref('')
const currentToolRequestMappings = ref<ParamMappingNode[]>([])
const editingPayload = ref<any>(null)

const page = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchData = async () => {
  if (!gatewayId) return
  loading.value = true
  try {
    const res = await listGatewayTools({ gatewayId, pageNum: page.pageNum, pageSize: page.pageSize })
    records.value = res.data.records || []
    page.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载工具失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingPayload.value = null
  editVisible.value = true
}

const openEdit = async (row: GatewayTool) => {
  if (!row.id) return
  try {
    const res = await getGatewayTool(row.id)
    editingPayload.value = res.data
    editVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载工具详情失败')
  }
}

const openDebug = async (row: GatewayTool) => {
  currentToolName.value = row.toolName
  currentToolRequestMappings.value = []
  if (row.id) {
    try {
      const res = await getGatewayTool(row.id)
      currentToolRequestMappings.value = Array.isArray(res.data?.requestMappings) ? res.data.requestMappings : []
    } catch (error: any) {
      ElMessage.warning(error.message || '加载参数模板失败，可手动输入')
    }
  }
  debugVisible.value = true
}

const removeTool = async (row: GatewayTool) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除工具 ${row.toolName}？`, '提示', { type: 'warning' })
    await deleteGatewayTool(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // ignore
  }
}

const toggleStatus = async (row: GatewayTool) => {
  if (!row.id) return
  try {
    if (row.status === 1) {
      await disableGatewayTool(row.id)
      ElMessage.success('已禁用')
    } else {
      await enableGatewayTool(row.id)
      ElMessage.success('已启用')
    }
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '状态变更失败')
  }
}

const normalizeTab = (value: unknown): 'tools' | 'credentials' => {
  return value === 'credentials' ? 'credentials' : 'tools'
}

const handleTabChange = (tabName: string | number) => {
  const nextTab = normalizeTab(String(tabName || ''))
  const nextQuery: Record<string, any> = { ...route.query }
  if (nextTab === 'credentials') {
    nextQuery.tab = 'credentials'
  } else {
    delete nextQuery.tab
  }
  router.replace({ path: route.path, query: nextQuery })
}

watch(
  () => route.query.tab,
  (value) => {
    const queryTab = Array.isArray(value) ? value[0] : value
    activeTab.value = normalizeTab(queryTab)
  },
  { immediate: true }
)

onMounted(fetchData)
</script>

<style scoped>
.gateway-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
