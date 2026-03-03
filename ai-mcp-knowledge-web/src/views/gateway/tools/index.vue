<template>
  <div class="gemini-container">
    <div class="page-header">
      <h2 class="page-title">HTTP 工具配置</h2>
      <div>
        <el-button
          v-if="activeTab === 'tools'"
          class="gemini-btn-primary"
          @click="refreshTools"
          :loading="refreshing"
        >
          刷新连通性
        </el-button>
        <el-button
          v-if="activeTab === 'tools'"
          class="gemini-btn-primary"
          @click="fetchBindingOverview"
          :loading="bindingOverviewLoading"
        >
          刷新绑定
        </el-button>
        <el-button
          v-if="activeTab === 'tools'"
          class="gemini-btn-primary"
          @click="bindingVisible = true"
        >
          配置绑定
        </el-button>
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

        <!-- 搜索栏 -->
        <el-card class="gemini-card" style="margin-bottom: 16px">
          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="工具名称">
              <el-input
                v-model="searchForm.toolNameKeyword"
                placeholder="请输入工具名称"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="工具描述">
              <el-input
                v-model="searchForm.toolDescriptionKeyword"
                placeholder="请输入工具描述"
                clearable
                style="width: 200px"
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button class="gemini-btn-primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

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
            <el-table-column label="绑定模型" min-width="260">
              <template #default="{ row }">
                <el-text v-if="bindingOverviewLoading" type="info">加载中...</el-text>
                <el-text v-else-if="row.status !== 1" type="info">工具已禁用</el-text>
                <el-text v-else-if="enabledModelCount === 0" type="info">暂无启用模型</el-text>
                <el-tag v-else-if="isToolVisibleToAllModels(row)" type="info">全部模型可见</el-tag>
                <el-text v-else-if="getToolVisibleModelNames(row).length === 0" type="info">未绑定</el-text>
                <div v-else class="binding-tags">
                  <el-tag v-for="name in getToolVisibleModelNames(row).slice(0, 3)" :key="`${row.id}-${name}`" size="small">
                    {{ name }}
                  </el-tag>
                  <el-tag v-if="getToolVisibleModelNames(row).length > 3" size="small" type="info">
                    +{{ getToolVisibleModelNames(row).length - 3 }}
                  </el-tag>
                </div>
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
      </el-tab-pane>
      <el-tab-pane label="网关凭证" name="credentials">
        <GatewayCredentialPanel :embedded="true" :default-gateway-id="gatewayId" />
      </el-tab-pane>
    </el-tabs>

    <ToolEditForm
      v-model:visible="editVisible"
      :gateway-id="gatewayId"
      :tool-data="editingPayload"
      @success="handleToolChanged"
    />

    <ToolDebugPanel
      v-model:visible="debugVisible"
      :gateway-id="gatewayId"
      :tool-name="currentToolName"
      :request-mappings="currentToolRequestMappings"
    />

    <ToolBindingDialog v-model:visible="bindingVisible" @success="handleBindingSuccess" />
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
  disableGatewayTool,
  refreshGatewayTools,
  listEnabledModels,
  getModelToolBindings
} from '@/api/gateway'
import ToolEditForm from './components/ToolEditForm.vue'
import ToolDebugPanel from './components/ToolDebugPanel.vue'
import ToolBindingDialog from '@/views/gateway/binding/ToolBindingDialog.vue'
import GatewayCredentialPanel from '@/views/gateway/credential/index.vue'
import type { GatewayTool, ModelOption, ParamMappingNode } from '@/types/gateway'

const DEFAULT_GATEWAY_ID = 'default_gateway'
const route = useRoute()
const router = useRouter()
const gatewayId = String(route.params.gatewayId || route.query.gatewayId || DEFAULT_GATEWAY_ID)
const activeTab = ref<'tools' | 'credentials'>('tools')

const loading = ref(false)
const refreshing = ref(false)
const records = ref<GatewayTool[]>([])
const bindingOverviewLoading = ref(false)
const enabledModelCount = ref(0)
const globalVisibleModelNames = ref<string[]>([])
const explicitVisibleModelNamesByToolId = ref<Record<number, string[]>>({})
const editVisible = ref(false)
const debugVisible = ref(false)
const bindingVisible = ref(false)
const currentToolName = ref('')
const currentToolRequestMappings = ref<ParamMappingNode[]>([])
const editingPayload = ref<any>(null)

// 搜索表单
const searchForm = reactive({
  toolNameKeyword: '',
  toolDescriptionKeyword: '',
  status: undefined as number | undefined
})

const page = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchData = async () => {
  if (!gatewayId) return
  loading.value = true
  try {
    const res = await listGatewayTools({
      gatewayId,
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      toolNameKeyword: searchForm.toolNameKeyword || undefined,
      toolDescriptionKeyword: searchForm.toolDescriptionKeyword || undefined,
      status: searchForm.status
    })
    records.value = res.data.records || []
    page.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载工具失败')
  } finally {
    loading.value = false
  }
}

const fetchBindingOverview = async () => {
  bindingOverviewLoading.value = true
  try {
    const modelsRes = await listEnabledModels()
    const models: ModelOption[] = modelsRes.data || []
    enabledModelCount.value = models.length

    const globalModels = new Set<string>()
    const explicitMap = new Map<number, Set<string>>()
    const bindingResults = await Promise.all(
      models.map(async (model) => {
        const bindingRes = await getModelToolBindings(model.id)
        return {
          modelName: model.modelName,
          globalVisible: !!bindingRes.data.globalVisible,
          toolIds: Array.isArray(bindingRes.data.toolIds) ? bindingRes.data.toolIds : []
        }
      })
    )

    for (const item of bindingResults) {
      if (item.globalVisible) {
        globalModels.add(item.modelName)
        continue
      }
      for (const toolId of item.toolIds) {
        if (typeof toolId !== 'number') {
          continue
        }
        let modelSet = explicitMap.get(toolId)
        if (!modelSet) {
          modelSet = new Set<string>()
          explicitMap.set(toolId, modelSet)
        }
        modelSet.add(item.modelName)
      }
    }

    const explicitRecord: Record<number, string[]> = {}
    for (const [toolId, modelSet] of explicitMap.entries()) {
      explicitRecord[toolId] = Array.from(modelSet)
    }
    globalVisibleModelNames.value = Array.from(globalModels)
    explicitVisibleModelNamesByToolId.value = explicitRecord
  } catch (error: any) {
    ElMessage.error(error.message || '加载绑定概览失败')
  } finally {
    bindingOverviewLoading.value = false
  }
}

const getToolVisibleModelNames = (tool: GatewayTool): string[] => {
  if (!tool?.id || tool.status !== 1) {
    return []
  }
  const explicitModels = explicitVisibleModelNamesByToolId.value[tool.id] || []
  return Array.from(new Set([...globalVisibleModelNames.value, ...explicitModels]))
}

const isToolVisibleToAllModels = (tool: GatewayTool): boolean => {
  if (enabledModelCount.value === 0) {
    return false
  }
  return getToolVisibleModelNames(tool).length === enabledModelCount.value
}

// 搜索处理
const handleSearch = () => {
  page.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.toolNameKeyword = ''
  searchForm.toolDescriptionKeyword = ''
  searchForm.status = undefined
  page.pageNum = 1
  fetchData()
}

// 刷新工具连通性
const refreshTools = async () => {
  refreshing.value = true
  try {
    const res = await refreshGatewayTools({ gatewayId })
    const { successCount, failedCount, refreshedCount } = res.data
    ElMessage.success(`刷新完成：共${refreshedCount}个工具，成功${successCount}个，失败${failedCount}个`)
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  } finally {
    refreshing.value = false
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
    fetchBindingOverview()
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
    fetchBindingOverview()
  } catch (error: any) {
    ElMessage.error(error.message || '状态变更失败')
  }
}

const handleToolChanged = () => {
  fetchData()
  fetchBindingOverview()
}

const handleBindingSuccess = () => {
  fetchData()
  fetchBindingOverview()
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

onMounted(() => {
  fetchData()
  fetchBindingOverview()
})
</script>

<style scoped>
.gateway-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.search-form .el-form-item {
  margin-bottom: 0;
  margin-right: 16px;
}

.binding-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
