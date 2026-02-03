<template>
  <div class="gemini-container">
    <el-card class="gemini-card">
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item>
          <el-button
            class="gemini-btn-secondary"
            @click="fetchData"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button
            type="primary"
            class="gemini-btn-primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            新增任务类型
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
          prop="taskName"
          label="任务名称"
          min-width="150"
        />
        <el-table-column
          prop="taskCode"
          label="任务编码"
          min-width="150"
        />
        <el-table-column
          label="首选模型"
          min-width="150"
        >
          <template #default="{ row }">
            {{ modelNameMap.get(row.preferredModelId) || row.preferredModelId }}
          </template>
        </el-table-column>
        <el-table-column
          label="备用模型"
          min-width="200"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ getFallbackNames(row.fallbackModelIds) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="描述"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          label="创建时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="200"
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

    <TaskForm
      v-model:visible="dialogVisible"
      :task-data="currentTask"
      :model-options="modelOptions"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskTypeList, deleteTaskType } from '@/api/task'
import { getModelList } from '@/api/model'
import TaskForm from './components/TaskForm.vue'
import type { ModelConfig, TaskType } from '@/types/entity'

interface ModelOption {
  id: number
  name: string
}

const loading = ref(false)
const tableData = ref<TaskType[]>([])
const dialogVisible = ref(false)
const currentTask = ref<TaskType | null>(null)
const modelOptions = ref<ModelOption[]>([])
const modelNameMap = ref(new Map<number, string>())

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const fetchModelOptions = async () => {
  try {
    const res = await getModelList({ pageNum: 1, pageSize: 100 })
    const records = res.data.records as ModelConfig[]
    modelOptions.value = records.map(item => ({
      id: item.id,
      name: item.modelName
    }))
    modelNameMap.value = new Map(modelOptions.value.map(item => [item.id, item.name]))
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型列表失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getTaskTypeList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '获取任务类型失败')
  } finally {
    loading.value = false
  }
}

const getFallbackNames = (fallbackModelIds?: string) => {
  if (!fallbackModelIds) return '-'
  const ids = fallbackModelIds
    .split(',')
    .map(id => id.trim())
    .filter(id => id)
    .map(id => Number(id))
    .filter(id => !Number.isNaN(id))
  if (ids.length === 0) return '-'
  return ids
    .map(id => modelNameMap.value.get(id) || `ID-${id}`)
    .join('、')
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

const handleAdd = () => {
  currentTask.value = null
  dialogVisible.value = true
}

const handleEdit = (row: TaskType) => {
  currentTask.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: TaskType) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务类型 "${row.taskName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteTaskType(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
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

const handleFormSuccess = () => {
  fetchData()
}

onMounted(() => {
  fetchModelOptions()
  fetchData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
</style>
