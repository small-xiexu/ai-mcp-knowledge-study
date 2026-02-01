<template>
  <div class="model-management">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-button type="primary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增模型
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="modelName" label="模型名称" min-width="150" />
        <el-table-column prop="modelType" label="模型类型" width="120" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="220" />
        <el-table-column label="对话激活" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.activeChat" type="success">已激活</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="嵌入激活" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.activeEmbedding" type="info">已激活</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="模型能力" min-width="200">
          <template #default="{ row }">
            <div v-if="row.capability">
              <div>Tokens: {{ row.capability.maxInputTokens }}</div>
              <div>Quality: {{ row.capability.qualityScore }}</div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-space>
                <el-button
                  type="primary"
                  size="small"
                  @click="handleEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  :type="row.enabled ? 'warning' : 'success'"
                  size="small"
                  @click="handleToggleStatus(row)"
                >
                  {{ row.enabled ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  type="warning"
                  size="small"
                  @click="handleTestConnection(row)"
                >
                  测试连接
                </el-button>
              </el-space>
            </div>
            <div class="action-row">
              <el-space>
                <el-button
                  type="success"
                  size="small"
                  @click="handleActivateChat(row)"
                >
                  激活对话
                </el-button>
                <el-button
                  type="info"
                  size="small"
                  @click="handleActivateEmbedding(row)"
                >
                  激活嵌入
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </el-space>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 表单对话框 -->
    <ModelForm
      v-model:visible="dialogVisible"
      :model-data="currentModel"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelList, deleteModel, enableModel, disableModel, activateChatModel, activateEmbeddingModel, testModelConnection } from '@/api/model'
import ModelForm from './components/ModelForm.vue'
import type { ModelConfig } from '@/types/entity'

const loading = ref(false)
const tableData = ref<ModelConfig[]>([])
const dialogVisible = ref(false)
const currentModel = ref<ModelConfig | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

// 获取模型列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getModelList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      sortField: 'updatedAt',
      sortOrder: 'DESC'
    })
    tableData.value = res.data.data.records
    pagination.total = res.data.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型列表失败')
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  currentModel.value = null
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: ModelConfig) => {
  currentModel.value = row
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: ModelConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型 "${row.modelName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteModel(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 切换状态
const handleToggleStatus = async (row: ModelConfig) => {
  try {
    if (row.enabled) {
      await disableModel(row.id)
      ElMessage.success('禁用成功')
    } else {
      await enableModel(row.id)
      ElMessage.success('启用成功')
    }
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleActivateChat = async (row: ModelConfig) => {
  try {
    await activateChatModel(row.id)
    ElMessage.success('对话模型激活成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '激活失败')
  }
}

const handleActivateEmbedding = async (row: ModelConfig) => {
  try {
    await activateEmbeddingModel(row.id)
    ElMessage.success('嵌入模型激活成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '激活失败')
  }
}

const handleTestConnection = async (row: ModelConfig) => {
  try {
    await testModelConnection(row.id)
    ElMessage.success('模型连接成功')
  } catch (error: any) {
    ElMessage.error(error.message || '模型连接失败')
  }
}

// 分页大小变化
const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

// 页码变化
const handleCurrentChange = () => {
  fetchData()
}

// 表单提交成功
const handleFormSuccess = () => {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.model-management {
  width: 100%;
}

.search-form {
  margin-bottom: 20px;
}

.action-row + .action-row {
  margin-top: 6px;
}
</style>
