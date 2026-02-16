<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Workflow 运行记录</h2>
        <p class="subtitle">最近 7 天运行明细会自动清理（服务端 Job）</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <div class="filters">
        <el-select v-model="query.status" clearable placeholder="status" style="width: 220px" @change="load">
          <el-option label="RUNNING" value="RUNNING" />
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="FAILED" value="FAILED" />
          <el-option label="PENDING_APPROVAL" value="PENDING_APPROVAL" />
        </el-select>
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <el-table :data="rows" stripe class="gemini-table" v-loading="loading" @row-click="onRowClick">
        <el-table-column prop="runId" label="runId" min-width="240" />
        <el-table-column prop="workflowCode" label="workflowCode" width="180" />
        <el-table-column prop="workflowVersionId" label="versionId" width="120" />
        <el-table-column prop="status" label="status" width="160">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="costMs" width="120" />
        <el-table-column prop="startedAt" label="startedAt" width="200" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openNodeRuns(row)">明细</el-button>
            <el-button link type="primary" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="prev, pager, next, sizes, total"
          :current-page="page.pageNum"
          :page-size="page.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="page.total"
          @update:current-page="onPageNum"
          @update:page-size="onPageSize"
        />
      </div>
    </div>

    <el-drawer v-model="drawer.visible" size="52%" :title="drawer.title" direction="rtl">
      <div v-loading="drawer.loading" style="height: 100%">
        <el-empty v-if="drawer.list.length === 0 && !drawer.loading" description="无节点运行明细" />
        <el-timeline v-else>
          <el-timeline-item v-for="n in drawer.list" :key="n.id" :type="timelineType(n.status)" :timestamp="n.nodeKey">
            <div class="node-run-card">
              <div class="nr-top">
                <div class="nr-name">{{ n.nodeName || n.nodeKey }}</div>
                <div class="nr-meta">
                  <el-tag size="small" :type="tagType(n.status)">{{ n.status }}</el-tag>
                  <span v-if="n.costMs != null" class="metric">{{ n.costMs }}ms</span>
                  <span v-if="n.totalTokens != null" class="metric">{{ n.totalTokens }} tok</span>
                  <span v-if="n.toolCallCount != null && n.toolCallCount > 0" class="metric">tool={{ n.toolCallCount }}</span>
                  <span v-if="n.toolDeniedCount != null && n.toolDeniedCount > 0" class="metric">deny={{ n.toolDeniedCount }}</span>
                </div>
              </div>
              <div class="nr-sub">{{ n.nodeType }}</div>
              <div v-if="n.errorMessage" class="nr-err">{{ n.errorMessage }}</div>
              <pre class="nr-output">{{ n.outputText || '' }}</pre>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { listWorkflowNodeRuns, listWorkflowRuns, type WorkflowNodeRun, type WorkflowRun } from '@/api/workflow'

const router = useRouter()

const loading = ref(false)
const rows = ref<WorkflowRun[]>([])
const query = reactive({ status: '' })
const page = reactive({ pageNum: 1, pageSize: 20, total: 0 })

const load = async () => {
  loading.value = true
  try {
    const offset = (Math.max(page.pageNum, 1) - 1) * Math.max(page.pageSize, 1)
    const res = await listWorkflowRuns({ status: query.status || undefined, offset, pageSize: page.pageSize })
    rows.value = res.data.records || []
    page.total = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const onPageNum = (v: number) => {
  page.pageNum = v
  load()
}
const onPageSize = (v: number) => {
  page.pageSize = v
  page.pageNum = 1
  load()
}

const drawer = reactive({
  visible: false,
  loading: false,
  title: '',
  runId: '',
  list: [] as WorkflowNodeRun[]
})

const openNodeRuns = async (row: WorkflowRun) => {
  if (!row?.runId) return
  drawer.visible = true
  drawer.loading = true
  drawer.title = `节点运行明细 | runId=${row.runId}`
  drawer.runId = row.runId
  try {
    const res = await listWorkflowNodeRuns({ runId: row.runId })
    drawer.list = (res.data || []).slice()
  } finally {
    drawer.loading = false
  }
}

const onRowClick = (row: WorkflowRun) => {
  if (!row?.runId) {
    ElMessage.error('缺少 runId')
    return
  }
  openNodeRuns(row)
}

const goDetail = (row: WorkflowRun) => {
  if (!row?.runId) return
  router.push({ name: 'WorkflowRunDetail', params: { runId: row.runId } })
}

const tagType = (status?: string) => {
  const s = (status || '').toUpperCase()
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'info'
  if (s === 'PENDING_APPROVAL') return 'warning'
  return ''
}

const timelineType = (status?: string) => {
  const s = (status || '').toUpperCase()
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'PENDING_APPROVAL') return 'warning'
  return 'primary'
}

load()
</script>

<style scoped lang="scss">
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.node-run-card {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.nr-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.nr-name {
  font-weight: 750;
}
.nr-meta {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.metric {
  font-size: 12px;
  color: var(--gemini-text-secondary);
}
.nr-sub {
  margin-top: 4px;
  color: var(--gemini-text-secondary);
  font-size: 12px;
}
.nr-err {
  margin-top: 8px;
  color: #ff8a8a;
  white-space: pre-wrap;
}
.nr-output {
  margin-top: 8px;
  max-height: 320px;
  overflow: auto;
  padding: 10px;
  background: rgba(0, 0, 0, 0.16);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}
</style>
