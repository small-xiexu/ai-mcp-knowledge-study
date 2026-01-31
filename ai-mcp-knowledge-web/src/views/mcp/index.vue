<template>
  <div class="mcp-management">
    <el-card>
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-button type="primary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="warning" @click="handleRuntimeRefresh">
            <el-icon><RefreshRight /></el-icon>
            刷新连接
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增 MCP
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="serverName" label="名称" min-width="160" />
        <el-table-column prop="serverType" label="类型" width="100" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行中" width="100">
          <template #default="{ row }">
            <el-tag :type="row.running ? 'success' : 'info'">
              {{ row.running ? '运行中' : '未运行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连接信息" min-width="220">
          <template #default="{ row }">
            <div v-if="row.serverType === 'STDIO'">
              <div>命令：{{ row.command || '-' }}</div>
            </div>
            <div v-else>
              <div>地址：{{ row.endpoint || '-' }}</div>
              <div v-if="row.sseEndpoint">SSE：{{ row.sseEndpoint }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              :type="row.enabled ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <McpServerForm
      v-model:visible="dialogVisible"
      :config-data="currentConfig"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMcpServerList,
  deleteMcpServer,
  enableMcpServer,
  disableMcpServer,
  refreshMcpServers
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
    tableData.value = res.data.data.records
    pagination.total = res.data.data.total
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

const handleEdit = (row: McpServerConfig) => {
  currentConfig.value = row
  dialogVisible.value = true
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
