<template>
  <div class="model-management-container">
    <div class="page-header">
      <h2 class="page-title">LLM 模型配置</h2>
      <el-button
        type="primary"
        class="add-btn"
        @click="handleAdd"
      >
        <el-icon><Plus /></el-icon>
        新增模型
      </el-button>
    </div>

    <div class="gemini-card">
      <!-- 搜索栏 -->
      <div class="table-toolbar">
        <el-button
          class="refresh-btn"
          circle
          @click="fetchData"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        class="gemini-table"
        :header-cell-style="{ background: 'transparent', color: '#9aa0a6', borderBottom: '1px solid #3c4043' }"
        :cell-style="{ background: 'transparent', color: '#e8eaed', borderBottom: '1px solid #3c4043' }"
        :row-class-name="tableRowClassName"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="modelName"
          label="模型名称"
          min-width="150"
        >
          <template #default="{ row }">
            <span class="model-name-text">{{ row.modelName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="modelType"
          label="类型"
          width="120"
        >
          <template #default="{ row }">
            <span class="model-type-badge">{{ row.modelType }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="baseUrl"
          label="Base URL"
          min-width="200"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="url-text">{{ row.baseUrl }}</span>
          </template>
        </el-table-column>
        
        <!-- 状态列合并展示 -->
        <el-table-column
          label="功能状态"
          min-width="240"
        >
          <template #default="{ row }">
            <div class="status-group">
              <span 
                class="status-dot-item" 
                :class="{ active: row.activeChat }"
                v-if="row.activeChat"
              >
                <span class="dot"></span>对话
              </span>
              <span 
                class="status-dot-item" 
                :class="{ active: row.activeEmbedding }"
                v-if="row.activeEmbedding"
              >
                <span class="dot"></span>嵌入
              </span>
              <span 
                class="status-dot-item" 
                :class="{ active: row.toolEnabled }"
                v-if="row.toolEnabled"
              >
                <span class="dot"></span>工具
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          label="全局状态"
          width="100"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              size="small"
              class="gemini-switch"
              style="--el-switch-on-color: #8ab4f8; --el-switch-off-color: #5f6368"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>

        <el-table-column
          prop="priority"
          label="优先级"
          width="80"
          align="center"
        />

        <el-table-column
          label="能力指标"
          min-width="160"
        >
          <template #default="{ row }">
            <div v-if="row.capability" class="capability-info">
              <div class="cap-item">
                <span class="label">Tokens</span>
                <span class="val">{{ row.capability.maxInputTokens }}</span>
              </div>
              <div class="cap-item">
                <span class="label">Quality</span>
                <el-progress 
                  :percentage="row.capability.qualityScore || 0" 
                  :show-text="false" 
                  :stroke-width="4"
                  color="#8ab4f8"
                  class="quality-progress"
                />
              </div>
            </div>
            <span v-else class="text-secondary">-</span>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="120"
          fixed="right"
          align="right"
        >
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                link
                class="action-btn"
                @click="handleEdit(row)"
              >
                <el-icon><EditPen /></el-icon>
              </el-button>
              
              <el-dropdown trigger="click" popper-class="gemini-dropdown">
                <el-button
                  type="primary"
                  link
                  class="action-btn"
                >
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleTestConnection(row)">
                      <el-icon><Connection /></el-icon>测试连接
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleActivateChat(row)">
                      <el-icon><ChatDotRound /></el-icon>设为对话模型
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleActivateEmbedding(row)">
                      <el-icon><Collection /></el-icon>设为嵌入模型
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDelete(row)">
                      <span class="text-danger"><el-icon><Delete /></el-icon>删除模型</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          class="gemini-pagination"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 表单对话框 -->
    <ModelForm
      v-model:visible="dialogVisible"
      :model-data="currentModel"
      @success="handleFormSuccess"
    />

    <ModelToolBinding style="margin-top: 16px;" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getModelList,
  getModelById,
  deleteModel,
  enableModel,
  disableModel,
  activateChatModel,
  activateEmbeddingModel,
  testModelConnection
} from '@/api/model'
import ModelForm from './components/ModelForm.vue'
import ModelToolBinding from './components/ModelToolBinding.vue'
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
    tableData.value = res.data.records
    pagination.total = res.data.total
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
const handleEdit = async (row: ModelConfig) => {
  dialogVisible.value = true
  currentModel.value = null
  try {
    const res = await getModelById(row.id)
    currentModel.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型配置失败')
  }
}

// 删除
const handleDelete = async (row: ModelConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型 "${row.modelName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
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

// 切换状态 - 修改为直接调用API，switch组件会自动更新UI
const handleToggleStatus = async (row: ModelConfig) => {
  const originalState = !row.enabled // 记录原始状态以便回滚
  try {
    if (row.enabled) {
      await enableModel(row.id)
      ElMessage.success('启用成功')
    } else {
      await disableModel(row.id)
      ElMessage.success('禁用成功')
    }
    // 不刷新列表，保持当前页状态
  } catch (error: any) {
    row.enabled = originalState // 恢复状态
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

const tableRowClassName = () => {
  return 'gemini-row';
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
/* 整体容器：深色背景 */
.model-management-container {
  width: 100%;
  min-height: 100%;
  padding: 24px;
  background-color: #131314; /* Gemini Dark Background */
  color: #e8eaed;
  box-sizing: border-box;
}

/* 顶部标题区 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .page-title {
    font-size: 24px;
    font-weight: 500;
    color: #e8eaed;
    margin: 0;
  }

  .add-btn {
    background: #8ab4f8;
    border: none;
    border-radius: 24px;
    padding: 10px 24px;
    font-weight: 500;
    color: #202124;
    transition: all 0.2s;

    &:hover {
      background: #aecbfa;
      transform: translateY(-1px);
    }
  }
}

/* 卡片容器：Glassmorphism */
.gemini-card {
  background: rgba(32, 33, 36, 0.6);
  border: 1px solid #3c4043;
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* 工具栏 */
.table-toolbar {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-bottom: 1px solid #3c4043;

  .refresh-btn {
    background: transparent;
    border: 1px solid #5f6368;
    color: #9aa0a6;
    
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #e8eaed;
    }
  }
}

/* 表格样式 */
.gemini-table {
  background: transparent !important;
  
  :deep(.el-table__inner-wrapper::before) {
    display: none; /*移除底部边框*/
  }
  
  :deep(.el-table__row) {
    transition: background 0.2s;
    
    &:hover {
      background-color: rgba(138, 180, 248, 0.08) !important;
    }
  }
}

.model-name-text {
  font-weight: 500;
  color: #e8eaed;
  font-size: 14px;
}

.model-type-badge {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(138, 180, 248, 0.15);
  color: #8ab4f8;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid rgba(138, 180, 248, 0.3);
}

.url-text {
  color: #9aa0a6;
  font-family: 'Roboto Mono', monospace;
  font-size: 12px;
}

/* 状态样式 */
.status-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.status-dot-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #9aa0a6;
  gap: 6px;
  
  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #5f6368;
  }
  
  &.active {
    color: #e8eaed;
    .dot {
      background-color: #81c995;
      box-shadow: 0 0 8px rgba(129, 201, 149, 0.4);
    }
  }
}

/* 能力指标 */
.capability-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  
  .cap-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #9aa0a6;
    
    .val {
      color: #e8eaed;
    }
  }
  
  .quality-progress {
    width: 60px;
    :deep(.el-progress-bar__outer) {
      background-color: #3c4043 !important;
    }
  }
}

.text-secondary {
  color: #5f6368;
}

.text-danger {
  color: #f28b82;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  
  .action-btn {
    color: #a8c7fa;
    font-size: 16px;
    padding: 4px;
    
    &:hover {
      color: #d2e3fc;
      background: rgba(168, 199, 250, 0.1);
      border-radius: 50%;
    }
  }
}

/* 分页 */
.pagination-container {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #3c4043;
}

.gemini-pagination {
  :deep(.el-pagination__total),
  :deep(.el-pagination__jump) {
    color: #9aa0a6;
  }
  
  :deep(.btn-prev),
  :deep(.btn-next) {
    background: transparent;
    color: #e8eaed;
    
    &:disabled {
      color: #5f6368;
    }
  }
  
  :deep(.el-pager li) {
    background: transparent;
    color: #9aa0a6;
    
    &.is-active {
      color: #8ab4f8;
      font-weight: bold;
    }
    
    &:hover {
      color: #e8eaed;
    }
  }
}
</style>

<style lang="scss">
/* 全局覆盖 (用于 Dropdown 等) */
.gemini-dropdown {
  background: #202124 !important;
  border: 1px solid #3c4043 !important;
  
  .el-dropdown-menu__item {
    color: #e8eaed !important;
    
    &:hover {
      background-color: rgba(255, 255, 255, 0.05) !important;
      color: #8ab4f8 !important;
    }
    
    &.el-dropdown-menu__item--divided {
      border-top-color: #3c4043 !important;
    }
  }
}
</style>
