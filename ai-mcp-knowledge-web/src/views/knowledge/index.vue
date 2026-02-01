<template>
  <div class="knowledge-page">
    <el-card>
      <div class="upload-section">
        <el-input v-model="ragTag" placeholder="请输入知识库标签" style="width: 240px" />
        <el-upload
          v-model:file-list="fileList"
          :auto-upload="false"
          :multiple="true"
          :before-upload="beforeUpload"
        >
          <el-button type="primary">选择文件</el-button>
        </el-upload>
        <el-button type="success" :loading="uploading" @click="handleUpload">
          上传
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="tag" label="知识库标签" min-width="200" />
        <el-table-column prop="count" label="向量数量" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.tag)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import { listRagTags, countRagTag, deleteRagTag, uploadRagFiles } from '@/api/rag'

interface TagRow {
  tag: string
  count: number
}

const ragTag = ref('')
const fileList = ref<UploadUserFile[]>([])
const tableData = ref<TagRow[]>([])
const loading = ref(false)
const uploading = ref(false)

const fetchTags = async () => {
  loading.value = true
  try {
    const res = await listRagTags()
    const tags = res.data.data || []
    const rows: TagRow[] = []
    for (const tag of tags) {
      const countRes = await countRagTag(tag)
      rows.push({ tag, count: countRes.data.data || 0 })
    }
    tableData.value = rows
  } catch (error: any) {
    ElMessage.error(error.message || '获取标签失败')
  } finally {
    loading.value = false
  }
}

const beforeUpload = (file: File) => {
  const sizeOk = file.size <= 30 * 1024 * 1024
  if (!sizeOk) {
    ElMessage.error('单文件大小不能超过 30MB')
    return false
  }
  return true
}

const handleUpload = async () => {
  if (!ragTag.value.trim()) {
    ElMessage.warning('请输入知识库标签')
    return
  }
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const files = fileList.value.map(item => item.raw as File).filter(Boolean)
    await uploadRagFiles(ragTag.value.trim(), files)
    ElMessage.success('上传成功')
    ragTag.value = ''
    fileList.value = []
    fetchTags()
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDelete = async (tag: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库 "${tag}" 吗？删除后将清空对应向量数据。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteRagTag(tag)
    ElMessage.success('删除成功')
    fetchTags()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.knowledge-page {
  width: 100%;
}

.upload-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
