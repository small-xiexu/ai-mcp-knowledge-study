<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">凭证管理</h2>
        <p class="subtitle">管理网关访问凭证，外部调用方鉴权使用。</p>
      </div>
    </div>

    <el-card class="gemini-card">
      <el-alert
        title="用途说明"
        type="warning"
        description="本页用于管理网关调用凭证（mcp_gateway_auth），用于外部客户端访问 MCP 网关时的鉴权与限流控制。"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="网关">
          <el-select
            v-model="searchForm.gatewayId"
            class="gemini-select"
            style="width: 220px"
            placeholder="请选择网关"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              v-for="gateway in gatewayOptions"
              :key="gateway.gatewayId"
              :label="`${gateway.gatewayName} (${gateway.gatewayId})`"
              :value="gateway.gatewayId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            class="gemini-select"
            style="width: 140px"
            clearable
            placeholder="全部"
            popper-class="gemini-select-dropdown"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item label="Key 关键字">
          <el-input
            v-model="searchForm.apiKeyKeyword"
            class="gemini-input"
            placeholder="支持包含搜索"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="gemini-btn-primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button class="gemini-btn-secondary" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button
            v-if="canManageCredentials"
            type="primary"
            class="gemini-btn-primary"
            @click="openCreate"
          >
            <el-icon><Plus /></el-icon>
            新增凭证
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" class="gemini-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="gatewayId" label="网关 ID" min-width="150" />
        <el-table-column label="API Key" min-width="280">
          <template #default="{ row }">
            <span class="mono-text">{{ row.apiKey }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" style="border: none">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rateLimit" label="限流(次/分钟)" width="140" />
        <el-table-column label="过期时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.expireTime) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="canManageCredentials" label="操作" width="120" align="center" header-align="center">
          <template #default="{ row }">
            <el-button
              text
              class="action-btn warning"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      title="新增网关凭证"
      width="560px"
      class="gemini-dialog"
      align-center
    >
      <el-form label-width="110px">
        <el-form-item label="网关">
          <el-select
            v-model="createForm.gatewayId"
            class="gemini-select"
            style="width: 100%"
            placeholder="请选择网关"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              v-for="gateway in gatewayOptions"
              :key="gateway.gatewayId"
              :label="`${gateway.gatewayName} (${gateway.gatewayId})`"
              :value="gateway.gatewayId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input
            v-model="createForm.apiKey"
            class="gemini-input"
            placeholder="可留空，系统自动生成"
          />
        </el-form-item>
        <el-form-item label="限流(次/分钟)">
          <el-input-number v-model="createForm.rateLimit" :min="1" :max="100000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="createForm.expireTime"
            type="datetime"
            class="gemini-input"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="gemini-btn-secondary" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="submitLoading" @click="handleSubmitCreate">
          创建
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createdDialogVisible"
      title="凭证创建成功"
      width="620px"
      class="gemini-dialog"
      align-center
    >
      <el-alert
        title="请妥善保存 API Key"
        type="warning"
        :closable="false"
        description="该 Key 将用于 MCP Gateway 鉴权，请仅分发给可信调用方。"
        style="margin-bottom: 12px"
      />
      <el-descriptions :column="1" border class="gemini-descriptions">
        <el-descriptions-item label="网关 ID">
          {{ createdCredential?.gatewayId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="API Key">
          <span class="mono-text">{{ createdCredential?.apiKey || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" class="gemini-btn-primary" @click="createdDialogVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  disableGatewayCredential,
  enableGatewayCredential,
  listGatewayCredentials,
  listGatewayInstances,
  saveGatewayCredential
} from '@/api/gateway'
import { usePermission } from '@/composables/usePermission'
import type { GatewayCredential, GatewayInstance } from '@/types/gateway'

const { hasPermission } = usePermission()
const canManageCredentials = computed(() => hasPermission('tool:write'))

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<GatewayCredential[]>([])
const gatewayOptions = ref<GatewayInstance[]>([])

const searchForm = reactive({
  gatewayId: 'default_gateway',
  status: undefined as number | undefined,
  apiKeyKeyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const createDialogVisible = ref(false)
const createdDialogVisible = ref(false)
const createdCredential = ref<GatewayCredential | null>(null)

const createForm = reactive({
  gatewayId: 'default_gateway',
  apiKey: '',
  rateLimit: 100,
  expireTime: ''
})

const fetchGateways = async () => {
  try {
    const res = await listGatewayInstances({
      pageNum: 1,
      pageSize: 200
    })
    gatewayOptions.value = (res.data.records || []) as GatewayInstance[]
    if (!gatewayOptions.value.some(item => item.gatewayId === searchForm.gatewayId)) {
      searchForm.gatewayId = gatewayOptions.value[0]?.gatewayId || 'default_gateway'
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取网关列表失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listGatewayCredentials({
      gatewayId: searchForm.gatewayId || 'default_gateway',
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: searchForm.status,
      apiKeyKeyword: searchForm.apiKeyKeyword || undefined
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '获取网关凭证列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.status = undefined
  searchForm.apiKeyKeyword = ''
  pagination.pageNum = 1
  fetchData()
}

const openCreate = () => {
  createForm.gatewayId = searchForm.gatewayId || 'default_gateway'
  createForm.apiKey = ''
  createForm.rateLimit = 100
  createForm.expireTime = ''
  createDialogVisible.value = true
}

const handleSubmitCreate = async () => {
  if (!createForm.gatewayId) {
    ElMessage.warning('请选择网关')
    return
  }
  submitLoading.value = true
  try {
    const payload = {
      gatewayId: createForm.gatewayId,
      apiKey: createForm.apiKey?.trim() || undefined,
      rateLimit: createForm.rateLimit,
      expireTime: createForm.expireTime || undefined,
      status: 1
    }
    const res = await saveGatewayCredential(payload)
    createdCredential.value = res.data
    createDialogVisible.value = false
    createdDialogVisible.value = true
    ElMessage.success('网关凭证创建成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '创建网关凭证失败')
  } finally {
    submitLoading.value = false
  }
}

const toggleStatus = async (row: GatewayCredential) => {
  if (!row.id) {
    return
  }
  try {
    const toDisable = row.status === 1
    await ElMessageBox.confirm(
      `确定要${toDisable ? '禁用' : '启用'}该网关凭证吗？`,
      '提示',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    if (toDisable) {
      await disableGatewayCredential(row.id)
      ElMessage.success('已禁用')
    } else {
      await enableGatewayCredential(row.id)
      ElMessage.success('已启用')
    }
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '状态更新失败')
    }
  }
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  await fetchGateways()
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.mono-text {
  font-family: monospace;
  word-break: break-all;
}

.gemini-descriptions {
  --el-descriptions-table-border: 1px solid var(--gemini-border);
  --el-descriptions-item-bordered-label-background: rgba(255, 255, 255, 0.05);
}
</style>
