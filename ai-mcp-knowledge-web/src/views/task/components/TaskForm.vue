<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑任务类型' : '新增任务类型'"
    width="600px"
    class="gemini-dialog"
    align-center
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item
        label="任务名称"
        prop="taskName"
      >
        <el-input
          v-model="formData.taskName"
          placeholder="请输入任务名称"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item
        label="任务编码"
        prop="taskCode"
      >
        <el-input
          v-model="formData.taskCode"
          placeholder="请输入任务编码"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item label="任务描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述（选填）"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item
        label="首选模型"
        prop="preferredModelId"
      >
        <el-select
          v-model="formData.preferredModelId"
          placeholder="请选择首选模型"
          style="width: 100%"
          class="gemini-select"
          popper-class="gemini-select-dropdown"
        >
          <el-option
            v-for="model in modelOptions"
            :key="model.id"
            :label="model.name"
            :value="model.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备用模型">
        <el-select
          v-model="formData.fallbackModelIds"
          multiple
          filterable
          placeholder="请选择备用模型（选填）"
          style="width: 100%"
          class="gemini-select"
          popper-class="gemini-select-dropdown"
        >
          <el-option
            v-for="model in modelOptions"
            :key="model.id"
            :label="model.name"
            :value="model.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" text class="cancel-btn">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          class="gemini-btn-primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createTaskType, updateTaskType } from '@/api/task'
import type { TaskType, TaskTypeRequest } from '@/types/entity'

interface ModelOption {
  id: number
  name: string
}

interface Props {
  visible: boolean
  taskData?: TaskType | null
  modelOptions: ModelOption[]
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = ref(false)

const formData = reactive({
  id: 0,
  taskName: '',
  taskCode: '',
  description: '',
  preferredModelId: undefined as number | undefined,
  fallbackModelIds: [] as number[]
})

const rules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskCode: [{ required: true, message: '请输入任务编码', trigger: 'blur' }],
  preferredModelId: [{ required: true, message: '请选择首选模型', trigger: 'change' }]
}

const parseFallbackIds = (fallbackModelIds?: string) => {
  if (!fallbackModelIds) return []
  return fallbackModelIds
    .split(',')
    .map(id => id.trim())
    .filter(id => id)
    .map(id => Number(id))
    .filter(id => !Number.isNaN(id))
}

const resetForm = () => {
  formData.id = 0
  formData.taskName = ''
  formData.taskCode = ''
  formData.description = ''
  formData.preferredModelId = undefined
  formData.fallbackModelIds = []
  formRef.value?.clearValidate()
}

watch(
  () => props.taskData,
  (data) => {
    if (data) {
      isEdit.value = true
      formData.id = data.id
      formData.taskName = data.taskName
      formData.taskCode = data.taskCode
      formData.description = data.description || ''
      formData.preferredModelId = data.preferredModelId
      formData.fallbackModelIds = parseFallbackIds(data.fallbackModelIds)
    } else {
      isEdit.value = false
      resetForm()
    }
  },
  { immediate: true }
)

const handleClose = () => {
  emit('update:visible', false)
  resetForm()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const payload: TaskTypeRequest = {
        id: isEdit.value ? formData.id : undefined,
        taskName: formData.taskName,
        taskCode: formData.taskCode,
        description: formData.description || undefined,
        preferredModelId: formData.preferredModelId as number,
        fallbackModelIds: formData.fallbackModelIds.length > 0
          ? formData.fallbackModelIds.join(',')
          : undefined
      }

      if (isEdit.value) {
        await updateTaskType(payload)
        ElMessage.success('任务类型更新成功')
      } else {
        await createTaskType(payload)
        ElMessage.success('任务类型创建成功')
      }

      emit('success')
      handleClose()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}
</script>
