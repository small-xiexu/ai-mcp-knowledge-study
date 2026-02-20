<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">运行详情</h2>
        <p class="subtitle">
          runId: <span class="mono">{{ runId }}</span>
        </p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <div class="meta-grid" v-loading="loadingRun">
        <div class="meta-item"><span class="k">workflowCode</span><span class="v mono">{{ run?.workflowCode || '-' }}</span></div>
        <div class="meta-item"><span class="k">status</span><span class="v"><el-tag size="small" :type="tagType(run?.status)">{{ run?.status || '-' }}</el-tag></span></div>
        <div class="meta-item"><span class="k">versionId</span><span class="v mono">{{ run?.workflowVersionId ?? '-' }}</span></div>
        <div class="meta-item"><span class="k">cost</span><span class="v">{{ run?.costMs != null ? `${run?.costMs}ms` : '-' }}</span></div>
        <div class="meta-item"><span class="k">startedAt</span><span class="v">{{ run?.startedAt || '-' }}</span></div>
        <div class="meta-item"><span class="k">endedAt</span><span class="v">{{ run?.endedAt || '-' }}</span></div>
        <div class="meta-item"><span class="k">currentNode</span><span class="v mono">{{ run?.currentNodeKey || '-' }}</span></div>
        <div class="meta-item"><span class="k">error</span><span class="v">{{ run?.errorMessage || '-' }}</span></div>
      </div>
    </div>

    <div class="gemini-card" style="margin-top: 14px">
      <div class="section-title">节点运行明细</div>
      <div v-loading="loadingNodes">
        <el-empty v-if="nodeRuns.length === 0 && !loadingNodes" description="无节点明细" />
        <el-timeline v-else>
          <el-timeline-item v-for="n in nodeRuns" :key="n.id" :type="timelineType(n.status)" :timestamp="n.nodeKey">
            <div class="node-run-card">
              <div class="nr-top">
                <div class="nr-name">{{ n.nodeName || n.nodeKey }}</div>
                <div class="nr-meta">
                  <el-tag size="small" :type="tagType(n.status)">{{ n.status }}</el-tag>
                  <span v-if="n.costMs != null" class="metric">{{ n.costMs }}ms</span>
                  <span v-if="n.toolCallCount != null && n.toolCallCount > 0" class="metric">tool={{ n.toolCallCount }}</span>
                  <span v-if="n.toolDeniedCount != null && n.toolDeniedCount > 0" class="metric">deny={{ n.toolDeniedCount }}</span>
                  <span v-if="n.outputTruncated === 1" class="metric warn">truncated</span>
                </div>
              </div>
              <div class="nr-sub">
                <span class="mono">{{ n.nodeType }}</span>
                <span v-if="n.modelNameUsed" class="metric">model={{ n.modelNameUsed }}</span>
                <span v-if="n.approvalRequestId" class="metric">approval=#{{ n.approvalRequestId }}</span>
              </div>
              <div v-if="n.errorMessage" class="nr-err">{{ n.errorMessage }}</div>
              <pre class="nr-output">{{ n.outputText || '' }}</pre>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getWorkflowRun, listWorkflowNodeRuns, type WorkflowNodeRun, type WorkflowRun } from '@/api/workflow'

const route = useRoute()
const runId = computed(() => String(route.params.runId || ''))

const loadingRun = ref(false)
const loadingNodes = ref(false)
const run = ref<WorkflowRun | null>(null)
const nodeRuns = ref<WorkflowNodeRun[]>([])

const load = async () => {
  if (!runId.value) return
  loadingRun.value = true
  loadingNodes.value = true
  try {
    const r = await getWorkflowRun(runId.value)
    run.value = r.data || null
  } finally {
    loadingRun.value = false
  }
  try {
    const n = await listWorkflowNodeRuns({ runId: runId.value })
    nodeRuns.value = (n.data || []).slice()
  } finally {
    loadingNodes.value = false
  }
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
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  color: var(--gemini-accent);
}
.section-title {
  font-weight: 750;
  margin-bottom: 12px;
}
.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.meta-item {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.k {
  width: 110px;
  color: var(--gemini-text-secondary);
  font-size: 12px;
}
.v {
  flex: 1;
  word-break: break-word;
  font-size: 12px;
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
.warn {
  color: #ffcc66;
}
.nr-sub {
  margin-top: 4px;
  display: flex;
  gap: 10px;
  align-items: center;
}
.nr-err {
  margin-top: 8px;
  color: #ff8a8a;
  white-space: pre-wrap;
}
.nr-output {
  margin-top: 8px;
  max-height: 420px;
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
