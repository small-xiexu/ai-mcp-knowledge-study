<template>
  <div class="task-management">
    <el-card>
      <el-form :inline="true" class="search-form">
        <el-form-item>
          <el-button type="primary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增任务类型
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
        <el-table-column prop="taskName" label="任务名称" min-width="160" />
        <el-table-column prop="taskCode" label="任务编码" width="140" />
        <el-table-column prop="preferredModelName" label="首选模型" width="160" />
        <el-table-column label="备用模型" min-width="220">
          <template #default="{ row }">
            <span>{{ getFallbackNames(row.fallbackModelIds) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
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
    const records = res.data.data.records as ModelConfig[]
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
    tableData.value = res.data.data.records
    pagination.total = res.data.data.total
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
.task-management {
  width: 100%;
}

.search-form {
  margin-bottom: 20px;
}
</style>
