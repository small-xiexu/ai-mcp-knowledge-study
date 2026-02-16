<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-alert
        title="用途说明"
        type="warning"
        description="本页用于配置和管理外部 MCP Server 连接（支持 STDIO/SSE/HTTP），并控制其运行状态。"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />

      <el-alert
        title="运行机制说明"
        type="info"
        description="【启用/禁用】控制运行权限，【开启连接】控制实际运行。启用后需手动点击“开启连接”方可生效；禁用会立即断开连接。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item>
          <el-button
            class="gemini-btn-secondary"
            :loading="loading"
            @click="fetchData"
          >
            <el-icon><Refresh /></el-icon>
            刷新列表
          </el-button>
          <el-button
            class="gemini-btn-secondary warning-border"
            @click="handleRuntimeRefresh"
          >
            <el-icon><RefreshRight /></el-icon>
            重启所有连接
          </el-button>
          <el-button
            type="primary"
            class="gemini-btn-primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            新增 MCP
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        class="gemini-table"
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="serverName"
          label="名称"
          min-width="160"
        />
        <el-table-column
          prop="serverType"
          label="类型"
          width="100"
        />
        <el-table-column
          label="启用"
          width="90"
        >
          <template #default="{ row }">
            <div class="status-indicator" :class="{ active: row.enabled }">
              <span class="dot"></span>
              {{ row.enabled ? '启用' : '禁用' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="运行中"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.running ? 'success' : 'info'" effect="dark" style="background: rgba(255,255,255,0.1); border: none;">
              {{ row.running ? '运行中' : '未运行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="连接信息"
          min-width="220"
        >
          <template #default="{ row }">
            <div v-if="row.serverType === 'STDIO'" style="color: var(--gemini-text-secondary); font-size: 13px;">
              <div>命令：<span style="color: var(--gemini-text-primary); font-family: monospace;">{{ row.command || '-' }}</span></div>
            </div>
            <div v-else style="color: var(--gemini-text-secondary); font-size: 13px;">
              <div>地址：<span style="color: var(--gemini-text-primary);">{{ row.endpoint || '-' }}</span></div>
              <div v-if="row.sseEndpoint">
                SSE：{{ row.sseEndpoint }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                text
                class="action-btn"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                text
                class="action-btn"
                :disabled="!row.enabled"
                @click="handleRefresh(row)"
              >
                开启连接
              </el-button>
              <el-button
                text
                class="action-btn"
                :class="row.enabled ? 'warning' : 'success'"
                @click="handleToggleStatus(row)"
              >
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-button
                text
                class="action-btn warning"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
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

    <McpServerForm
      v-model:visible="dialogVisible"
      :config-data="currentConfig"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshRight } from '@element-plus/icons-vue'
import {
  getMcpServerList,
  getMcpServerById,
  deleteMcpServer,
  enableMcpServer,
  disableMcpServer,
  refreshMcpServers,
  refreshMcpServer
} from '@/api/mcp'
import McpServerForm from './components/McpServerForm.vue'
import type { McpServerConfig } from '@/types/entity'

const loading = ref(false)
const tableData = ref<McpServerConfig[]>([])
const dialogVisible = ref(false)
const currentConfig = ref<McpServerConfig | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMcpServerList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '获取 MCP 配置失败')
  } finally {
    loading.value = false
  }
}

const handleRuntimeRefresh = async () => {
  try {
    await refreshMcpServers()
    ElMessage.success('运行时连接已刷新')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  }
}

const handleAdd = () => {
  currentConfig.value = null
  dialogVisible.value = true
}

const handleEdit = async (row: McpServerConfig) => {
  dialogVisible.value = true
  currentConfig.value = null

  try {
    const res = await getMcpServerById(row.id)
    currentConfig.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取 MCP 配置失败')
  }
}

const handleDelete = async (row: McpServerConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 MCP Server "${row.serverName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteMcpServer(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleToggleStatus = async (row: McpServerConfig) => {
  try {
    if (row.enabled) {
      await disableMcpServer(row.id)
      ElMessage.success('禁用成功')
    } else {
      await enableMcpServer(row.id)
      ElMessage.success('启用成功')
    }
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleRefresh = async (row: McpServerConfig) => {
  try {
    await refreshMcpServer(row.id)
    ElMessage.success('运行时连接已刷新')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '刷新失败')
  }
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

const handleFormSuccess = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>
