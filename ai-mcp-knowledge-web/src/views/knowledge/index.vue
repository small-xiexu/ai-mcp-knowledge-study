<template>
  <div class="gemini-container knowledge-page">
    <header class="page-header">
      <div class="title-with-icon">
        <h2 class="page-title">知识库管理</h2>
        <p class="subtitle">管理私有知识库标签并异步上传文档进行 RAG 处理</p>
      </div>
    </header>

    <div class="content-body">
      <!-- 统一的上传面板 -->
      <section class="gemini-card upload-panel">
        <div class="panel-header">
          <el-icon><Files /></el-icon>
          <span>导入文档</span>
        </div>
        
        <div class="panel-body">
          <div class="tag-input-wrapper">
             <span class="field-label">库标签</span>
             <el-input
               v-model="ragTag"
               placeholder="例如: java-docs"
               class="gemini-input"
             />
          </div>

          <div class="file-action-group">
            <el-upload
              v-model:file-list="fileList"
              :auto-upload="false"
              :multiple="true"
              :before-upload="beforeUpload"
              :show-file-list="true"
              class="gemini-uploader"
            >
              <template #trigger>
                <el-button class="gemini-btn-secondary">
                  <el-icon><Plus /></el-icon>
                  选择文件
                </el-button>
              </template>
            </el-upload>
            
            <el-button
              class="gemini-btn-primary upload-submit-btn"
              :loading="uploading"
              :disabled="!ragTag.trim() || fileList.length === 0"
              @click="handleUpload"
            >
              <el-icon v-if="!uploading"><Top /></el-icon>
              开始异步处理
            </el-button>
          </div>
          
          <div class="upload-tip">
            支持多个文件同时上传，单文件上限 30MB。建议使用 PDF, Markdown 或文本文件。
          </div>
        </div>
      </section>

      <!-- 知识库列表 -->
      <section class="gemini-card table-panel">
        <div class="panel-header">
          <el-icon><List /></el-icon>
          <span>现有知识库 ({{ tableData.length }})</span>
        </div>
        
        <el-table
          v-loading="loading"
          :data="tableData"
          class="gemini-table"
        >
          <el-table-column
            prop="tag"
            label="标签名称"
            min-width="180"
          >
            <template #default="{ row }">
              <span class="tag-name-bold">{{ row.tag }}</span>
            </template>
          </el-table-column>
          <el-table-column
            prop="count"
            label="向量点位"
            width="140"
            align="center"
          >
            <template #default="{ row }">
              <el-tag size="small" effect="dark" class="count-tag">
                {{ row.count }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="120"
            align="center"
            header-align="center"
          >
            <template #default="{ row }">
              <el-button
                link
                class="action-btn-danger"
                @click="handleDelete(row.tag)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </template>
          </el-table-column>
          
          <template #empty>
            <div class="empty-placeholder">
              <el-icon><FolderOpened /></el-icon>
              <p>暂无活跃知识库</p>
            </div>
          </template>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type UploadUserFile } from 'element-plus'
import { Files, Plus, Top, List, Delete, FolderOpened } from '@element-plus/icons-vue'
import { listRagTags, countRagTag, deleteRagTag, uploadFilesAsync } from '@/api/rag'

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
    const tags = res.data || []
    const rows: TagRow[] = []
    for (const tag of tags) {
      const countRes = await countRagTag(tag)
      rows.push({ tag, count: countRes.data || 0 })
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
    const res = await uploadFilesAsync(ragTag.value.trim(), files)
    const taskId = res.data
    ElMessage.success(`上传任务已创建: ${taskId}，请在任务列表查看进度`)
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

.content-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Panel Header */
.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--gemini-border);
  color: var(--gemini-text-primary);
  font-weight: 500;
  font-size: 15px;
}

.panel-header .el-icon {
  color: var(--gemini-accent);
}

.panel-body {
  padding: 24px;
}

/* Upload Panel Styles */
.tag-input-wrapper {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: var(--gemini-text-secondary);
  font-weight: 500;
}

.tag-input-wrapper :deep(.el-input) {
  max-width: 320px;
}

.file-action-group {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.gemini-uploader {
  flex: 0 0 auto;
}

.upload-submit-btn {
  height: 40px;
  padding: 0 24px;
  border-radius: 20px;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: var(--gemini-text-secondary);
  opacity: 0.8;
  margin-top: 8px;
}

/* Table Enhancements */
.tag-name-bold {
  font-weight: 600;
  color: var(--gemini-text-primary);
}

.count-tag {
  background-color: rgba(138, 180, 248, 0.1) !important;
  border: 1px solid rgba(138, 180, 248, 0.2) !important;
  color: var(--gemini-accent) !important;
  font-weight: 700;
}

.action-btn-danger {
  color: var(--gemini-danger) !important;
  font-size: 13px;
  gap: 4px;
}

.action-btn-danger:hover {
  opacity: 0.8;
}

/* Empty State */
.empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: var(--gemini-text-secondary);
  gap: 12px;
}

.empty-placeholder .el-icon {
  font-size: 48px;
  opacity: 0.2;
}

.empty-placeholder p {
  font-size: 14px;
  margin: 0;
}

:deep(.el-upload-list) {
  margin-top: 12px;
  max-width: 400px;
}

:deep(.el-upload-list__item) {
  background-color: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid var(--gemini-border) !important;
  border-radius: 8px !important;
  color: var(--gemini-text-secondary) !important;
}
</style>
