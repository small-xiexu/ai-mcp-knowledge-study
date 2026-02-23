<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Workflow 画布编辑</h2>
        <p class="subtitle">{{ titleText }}</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="load">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button class="gemini-btn-secondary" @click="openAgentEnhancerDlg">
          <el-icon><Setting /></el-icon>
          Agent 增强器
        </el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="saving" @click="save">
          <el-icon><Download /></el-icon>
          保存图
        </el-button>
      </div>
    </div>

    <div class="gemini-card editor-shell">
      <div class="left-panel">
        <div class="panel-title">节点库</div>
        <div class="palette">
          <el-button v-for="t in nodeTypes" :key="t" class="palette-btn" @click="addNode(t)">{{ t }}</el-button>
        </div>

        <div class="panel-title" style="margin-top: 12px">工具</div>
        <div class="tools">
          <el-switch v-model="mode.pan" active-text="拖动画布" inactive-text="拖动节点" />
          <el-switch v-model="mode.connect" active-text="连线模式" inactive-text="选择模式" />
          <div class="tool-row">
            <el-button class="gemini-btn-secondary" @click="zoomOut">-</el-button>
            <div class="zoom-label">{{ Math.round(view.zoom * 100) }}%</div>
            <el-button class="gemini-btn-secondary" @click="zoomIn">+</el-button>
          </div>
          <el-button class="gemini-btn-secondary" @click="resetView">重置视图</el-button>
        </div>

        <div class="panel-title" style="margin-top: 12px">连线提示</div>
        <div class="hint">
          <div v-if="mode.connect && !connectState.sourceKey">点击一个节点作为 source</div>
          <div v-else-if="mode.connect && connectState.sourceKey">再点击一个节点作为 target（source={{ connectState.sourceKey }}）</div>
          <div v-else>关闭连线模式可选中节点/边并编辑</div>
        </div>
      </div>

      <div class="stage-wrap">
        <div class="stage" ref="stageRef" @pointerdown="onStagePointerDown" @pointermove="onStagePointerMove" @pointerup="onStagePointerUp" @pointercancel="onStagePointerUp">
          <div class="grid" />
          <div class="viewport" :style="viewportStyle">
            <svg class="edges" :width="stageSize.w" :height="stageSize.h">
              <path
                v-for="e in edges"
                :key="edgeKey(e)"
                :d="edgePath(e)"
                class="edge-path"
                :class="{ selected: selected.kind === 'edge' && selected.key === edgeKey(e) }"
                @click.stop="selectEdge(e)"
              />
            </svg>

            <div
              v-for="n in nodes"
              :key="n.nodeKey"
              class="node"
              :class="{
                selected: selected.kind === 'node' && selected.key === n.nodeKey,
                start: n.nodeType === 'START',
                end: n.nodeType === 'END' || n.nodeType === 'OUTPUT'
              }"
              :style="nodeStyle(n)"
              @pointerdown.stop="onNodePointerDown($event, n)"
              @click.stop="onNodeClick(n)"
            >
              <div class="node-hd">
                <div class="node-type">{{ n.nodeType }}</div>
                <div class="node-name">{{ n.nodeName || n.nodeKey }}</div>
              </div>
              <div class="node-ft">
                <span class="node-key">{{ n.nodeKey }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="panel-title">属性</div>

        <el-empty v-if="selected.kind === 'none'" description="未选择节点/边" />

        <div v-else-if="selected.kind === 'node' && activeNode" class="inspector">
          <div class="inspector-row">
            <div class="lbl">nodeKey</div>
            <div class="val mono">{{ activeNode.nodeKey }}</div>
          </div>
          <div class="inspector-row">
            <div class="lbl">nodeType</div>
            <el-select v-model="activeNode.nodeType" style="width: 100%">
              <el-option v-for="t in nodeTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <div class="inspector-row">
            <div class="lbl">nodeName</div>
            <el-input v-model="activeNode.nodeName" placeholder="可选" />
          </div>

          <div class="inspector-row" v-if="wizard.visible">
            <div class="lbl">向导</div>
            <div class="wizard">
              <template v-if="wizard.kind === 'LLM' || wizard.kind === 'OUTPUT'">
                <el-form label-width="110px" class="wizard-form">
                  <el-form-item label="systemPrompt">
                    <el-input v-model="wizard.form.systemPrompt" type="textarea" :rows="3" placeholder="可选" />
                  </el-form-item>
                  <el-form-item label="userTemplate">
                    <el-input v-model="wizard.form.userTemplate" type="textarea" :rows="3" placeholder="默认 {{input}}" />
                  </el-form-item>
                  <el-form-item label="toolEnabled">
                    <el-switch v-model="wizard.form.toolEnabled" />
                  </el-form-item>
                  <el-form-item label="allowToolKeys">
                    <el-input v-model="wizard.form.allowedToolKeysJson" placeholder='JSON 数组，例如 ["tool.a","tool.b"]' />
                  </el-form-item>
                  <el-form-item label="ragFromNodeKey">
                    <el-input v-model="wizard.form.ragFromNodeKey" placeholder="可选：引用某个 RAG 节点输出" />
                  </el-form-item>
                  <el-form-item v-if="wizard.kind === 'OUTPUT'" label="outputMode">
                    <el-select v-model="wizard.form.outputMode" style="width: 100%">
                      <el-option label="CONTRACT" value="CONTRACT" />
                      <el-option label="TEXT" value="TEXT" />
                    </el-select>
                  </el-form-item>
                </el-form>
              </template>

              <template v-else-if="wizard.kind === 'TOOL_CALL'">
                <el-form label-width="110px" class="wizard-form">
                  <el-form-item label="toolKey">
                    <el-input v-model="wizard.form.toolKey" placeholder="必填" />
                  </el-form-item>
                  <el-form-item label="argsTemplate">
                    <el-input v-model="wizard.form.argumentsTemplateJson" type="textarea" :rows="4" placeholder='JSON 模板，支持 {{input}}/{{vars}}/{{steps}}' />
                  </el-form-item>
                  <el-form-item label="allowToolKeys">
                    <el-input v-model="wizard.form.allowedToolKeysJson" placeholder='JSON 数组；不填默认仅允许自身 toolKey' />
                  </el-form-item>
                  <el-form-item>
                    <el-button class="gemini-btn-secondary" @click="wizardFillToolAllowSelf">仅允许自身</el-button>
                  </el-form-item>
                </el-form>
              </template>

              <template v-else-if="wizard.kind === 'RAG_RETRIEVE'">
                <el-form label-width="110px" class="wizard-form">
                  <el-form-item label="queryTemplate">
                    <el-input v-model="wizard.form.queryTemplate" placeholder="默认 {{input}}" />
                  </el-form-item>
                  <el-form-item label="ragTags">
                    <el-input v-model="wizard.form.ragTagsJson" placeholder='JSON 数组，例如 ["tag1","tag2"]' />
                  </el-form-item>
                </el-form>
              </template>

              <template v-else-if="wizard.kind === 'IF'">
                <el-form label-width="110px" class="wizard-form">
                  <el-form-item label="expr">
                    <el-input v-model="wizard.form.ifExpr" placeholder='例如: ${vars.foo}==\"bar\"' />
                  </el-form-item>
                  <el-form-item label="varPath">
                    <el-input v-model="wizard.form.ifVarPath" placeholder="可选：vars.someKey / steps.nodeKey.xxx" />
                  </el-form-item>
                  <el-form-item label="equals">
                    <el-input v-model="wizard.form.ifEquals" placeholder="可选：与 varPath 配合" />
                  </el-form-item>
                </el-form>
              </template>

              <div class="cfg-actions">
                <el-button class="gemini-btn-secondary" @click="wizardLoadFromJson">从 JSON 载入</el-button>
                <el-button class="gemini-btn-secondary" @click="wizardApplyToJson">写回 JSON</el-button>
              </div>
            </div>
          </div>

          <div class="inspector-row">
            <div class="lbl">configJson</div>
            <el-input v-model="activeNode.configJson" type="textarea" :rows="12" placeholder="节点配置 JSON（LLM/TOOL_CALL/RAG/IF/OUTPUT...）" />
            <div class="cfg-actions">
              <el-button class="gemini-btn-secondary" @click="formatNodeJson(activeNode)">格式化</el-button>
              <el-button class="gemini-btn-secondary" @click="validateNodeJson(activeNode)">校验</el-button>
              <el-button class="gemini-btn-danger" @click="removeNode(activeNode.nodeKey)">删除节点</el-button>
            </div>
          </div>
        </div>

        <div v-else-if="selected.kind === 'edge' && activeEdge" class="inspector">
          <div class="inspector-row">
            <div class="lbl">source</div>
            <div class="val mono">{{ activeEdge.sourceKey }}</div>
          </div>
          <div class="inspector-row">
            <div class="lbl">target</div>
            <div class="val mono">{{ activeEdge.targetKey }}</div>
          </div>
          <div class="inspector-row">
            <div class="lbl">edgeType</div>
            <el-select v-model="activeEdge.edgeType" style="width: 100%">
              <el-option label="DEFAULT" value="DEFAULT" />
              <el-option label="TRUE" value="TRUE" />
              <el-option label="FALSE" value="FALSE" />
              <el-option label="CONDITION" value="CONDITION" />
            </el-select>
          </div>
          <div class="inspector-row" v-if="(activeEdge.edgeType || 'DEFAULT') === 'CONDITION'">
            <div class="lbl">conditionExpr</div>
            <el-input v-model="activeEdge.conditionExpr" type="textarea" :rows="6" placeholder='例如: ${vars.foo}==\"bar\"' />
          </div>
          <div class="cfg-actions">
            <el-button class="gemini-btn-danger" @click="removeEdge(activeEdge)">删除边</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="agentEnhancerDlg.visible" title="Workflow Agent 增强器绑定" width="860px" class="gemini-dialog">
      <div class="muted" style="margin-bottom: 10px">
        bindType=<span class="mono">WORKFLOW_VERSION</span>, bindTargetId=<span class="mono">#{{ workflowVersionId }}</span>
      </div>
      <div class="bind-row">
        <el-select v-model="agentEnhancerDlg.pickerId" filterable clearable placeholder="选择 Agent 增强器" style="width: 100%">
          <el-option
            v-for="a in agentEnhancerDlg.options"
            :key="a.id"
            :label="`${a.agentEnhancerCode} | ${a.agentEnhancerType} | ${a.agentEnhancerName}`"
            :value="a.id"
          />
        </el-select>
        <el-button class="gemini-btn-secondary" style="margin-left: 10px" @click="agentEnhancerAdd">添加</el-button>
      </div>

      <div class="bind-list" v-if="agentEnhancerDlg.items.length">
        <div v-for="(it, idx) in agentEnhancerDlg.items" :key="it.agentEnhancerId" class="bind-item">
          <div class="bind-left">
            <div class="bind-title">{{ agentEnhancerLabel(it.agentEnhancerId) }}</div>
            <div class="bind-sub muted">order={{ idx }}</div>
          </div>
          <div class="bind-right">
            <el-switch v-model="it.enabled" active-text="启用" inactive-text="禁用" />
            <el-button class="gemini-btn-secondary" @click="agentEnhancerMoveUp(idx)">上移</el-button>
            <el-button class="gemini-btn-secondary" @click="agentEnhancerMoveDown(idx)">下移</el-button>
            <el-button class="gemini-btn-danger" @click="agentEnhancerRemove(idx)">移除</el-button>
          </div>
        </div>
      </div>
      <div v-else class="muted" style="margin-top: 10px">未绑定 Agent 增强器（默认仅注入全局 TraceIdAgentEnhancer）。</div>

      <template #footer>
        <el-button class="gemini-btn-secondary" @click="agentEnhancerDlg.visible = false">关闭</el-button>
        <el-button class="gemini-btn-secondary" :loading="agentEnhancerDlg.preheating" @click="preheatWorkflow">预热</el-button>
        <el-button type="primary" class="gemini-btn-primary" :loading="agentEnhancerDlg.saving" @click="advisorSave">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getWorkflow, listWorkflowVersions, saveWorkflowGraph, type Workflow, type WorkflowGraphEdge, type WorkflowGraphNode, type WorkflowVersion } from '@/api/workflow'
import { listAgentEnhancerBindings, listAgentEnhancers, saveAgentEnhancerBindings, type AgentEnhancer } from '@/api/agent-enhancer'
import { preheatWorkflowVersion } from '@/api/preheat'
import { Download, Refresh, Setting } from '@element-plus/icons-vue'

type NodeVM = WorkflowGraphNode & { positionX: number; positionY: number; configJson: string; nodeName: string; nodeType: string; nodeKey: string }
type EdgeVM = WorkflowGraphEdge & { sourceKey: string; targetKey: string; edgeType: string; conditionExpr?: string }

const route = useRoute()

const workflowCode = computed(() => String(route.params.workflowCode || ''))
const workflowId = computed(() => {
  const raw = route.query.workflowId
  return raw ? Number(raw) : 0
})
const workflowVersionId = computed(() => {
  const raw = route.query.workflowVersionId
  return raw ? Number(raw) : 0
})

const wf = ref<Workflow | null>(null)
const version = ref<WorkflowVersion | null>(null)

const titleText = computed(() => {
  const v = version.value
  const w = wf.value
  const left = w ? `${w.workflowName} (${w.workflowCode})` : (workflowCode.value || '-')
  const right = v ? `v${v.versionNo} (${v.state})` : (workflowVersionId.value ? `versionId=${workflowVersionId.value}` : '未指定版本')
  return `${left} | ${right}`
})

const agentEnhancerDlg = reactive({
  visible: false,
  saving: false,
  preheating: false,
  options: [] as AgentEnhancer[],
  pickerId: undefined as number | undefined,
  items: [] as Array<{ agentEnhancerId: number; enabled: boolean }>
})

const agentEnhancerLabel = (agentEnhancerId: number) => {
  const a = agentEnhancerDlg.options.find(x => x.id === agentEnhancerId)
  if (!a) return `#${agentEnhancerId}`
  return `${a.agentEnhancerCode} | ${a.agentEnhancerType} | ${a.agentEnhancerName}`
}

const openAgentEnhancerDlg = async () => {
  if (!workflowVersionId.value) {
    ElMessage.error('缺少 workflowVersionId')
    return
  }
  agentEnhancerDlg.visible = true
  agentEnhancerDlg.pickerId = undefined
  agentEnhancerDlg.saving = false
  try {
    const res = await listAgentEnhancers({ pageNum: 1, pageSize: 200, enabled: true })
    agentEnhancerDlg.options = res.data?.records || []
  } catch {
    agentEnhancerDlg.options = []
  }
  try {
    const res = await listAgentEnhancerBindings({ bindType: 'WORKFLOW_VERSION', bindTargetId: workflowVersionId.value })
    const list = res.data || []
    agentEnhancerDlg.items = list
      .slice()
      .sort((a, b) => (a.orderNo || 0) - (b.orderNo || 0))
      .map(v => ({ agentEnhancerId: v.agentEnhancerId, enabled: v.bindingEnabled === 1 }))
  } catch {
    agentEnhancerDlg.items = []
  }
}

const agentEnhancerAdd = () => {
  if (!agentEnhancerDlg.pickerId) {
    ElMessage.warning('请选择 Agent 增强器')
    return
  }
  const exists = agentEnhancerDlg.items.some(x => x.agentEnhancerId === agentEnhancerDlg.pickerId)
  if (exists) {
    ElMessage.warning('已绑定该 Agent 增强器')
    return
  }
  agentEnhancerDlg.items.push({ agentEnhancerId: agentEnhancerDlg.pickerId, enabled: true })
  agentEnhancerDlg.pickerId = undefined
}

const agentEnhancerRemove = (idx: number) => {
  agentEnhancerDlg.items.splice(idx, 1)
}

const agentEnhancerMoveUp = (idx: number) => {
  if (idx <= 0) return
  const arr = agentEnhancerDlg.items
  const tmp = arr[idx - 1]
  arr[idx - 1] = arr[idx]
  arr[idx] = tmp
}

const agentEnhancerMoveDown = (idx: number) => {
  const arr = agentEnhancerDlg.items
  if (idx < 0 || idx >= arr.length - 1) return
  const tmp = arr[idx + 1]
  arr[idx + 1] = arr[idx]
  arr[idx] = tmp
}

const advisorSave = async () => {
  if (!workflowVersionId.value) return
  agentEnhancerDlg.saving = true
  try {
    await saveAgentEnhancerBindings({
      bindType: 'WORKFLOW_VERSION',
      bindTargetId: workflowVersionId.value,
      items: (agentEnhancerDlg.items || []).map((it, idx) => ({ agentEnhancerId: it.agentEnhancerId, orderNo: idx, enabled: it.enabled }))
    })
    ElMessage.success('保存成功')
  } finally {
    agentEnhancerDlg.saving = false
  }
}

const preheatWorkflow = async () => {
  if (!workflowVersionId.value) return
  agentEnhancerDlg.preheating = true
  try {
    const res = await preheatWorkflowVersion({ workflowVersionId: workflowVersionId.value, refreshMcp: false })
    const warnings = res.data?.warnings || []
    if (warnings.length) {
      ElMessage.warning(`预热完成（有告警 ${warnings.length} 条）`)
    } else {
      ElMessage.success('预热完成')
    }
  } finally {
    agentEnhancerDlg.preheating = false
  }
}

const nodeTypes = ['START', 'LLM', 'OUTPUT', 'TOOL_CALL', 'RAG_RETRIEVE', 'IF', 'PARALLEL', 'JOIN', 'END']

const nodes = ref<NodeVM[]>([])
const edges = ref<EdgeVM[]>([])

const view = reactive({ zoom: 1, x: 0, y: 0 })
const mode = reactive({ pan: false, connect: false })
const connectState = reactive<{ sourceKey: string }>({ sourceKey: '' })

const selected = reactive<{ kind: 'none' | 'node' | 'edge'; key: string }>({ kind: 'none', key: '' })
const activeNode = computed(() => (selected.kind === 'node' ? nodes.value.find(n => n.nodeKey === selected.key) || null : null))
const activeEdge = computed(() => (selected.kind === 'edge' ? edges.value.find(e => edgeKey(e) === selected.key) || null : null))

const wizard = reactive({
  visible: false,
  kind: '' as '' | 'LLM' | 'OUTPUT' | 'TOOL_CALL' | 'RAG_RETRIEVE' | 'IF',
  form: {
    systemPrompt: '',
    userTemplate: '{{input}}',
    toolEnabled: true,
    allowedToolKeysJson: '',
    ragFromNodeKey: '',
    outputMode: 'CONTRACT',
    toolKey: '',
    argumentsTemplateJson: '{}',
    queryTemplate: '{{input}}',
    ragTagsJson: '[]',
    ifExpr: '',
    ifVarPath: '',
    ifEquals: ''
  }
})

const detectWizardKind = (nodeType?: string) => {
  const t = String(nodeType || '').toUpperCase()
  if (t === 'LLM') return 'LLM'
  if (t === 'OUTPUT') return 'OUTPUT'
  if (t === 'TOOL_CALL') return 'TOOL_CALL'
  if (t === 'RAG_RETRIEVE') return 'RAG_RETRIEVE'
  if (t === 'IF') return 'IF'
  return ''
}

const wizardReset = () => {
  wizard.form.systemPrompt = ''
  wizard.form.userTemplate = '{{input}}'
  wizard.form.toolEnabled = true
  wizard.form.allowedToolKeysJson = ''
  wizard.form.ragFromNodeKey = ''
  wizard.form.outputMode = 'CONTRACT'
  wizard.form.toolKey = ''
  wizard.form.argumentsTemplateJson = '{}'
  wizard.form.queryTemplate = '{{input}}'
  wizard.form.ragTagsJson = '[]'
  wizard.form.ifExpr = ''
  wizard.form.ifVarPath = ''
  wizard.form.ifEquals = ''
}

const wizardLoadFromJson = () => {
  if (!activeNode.value) return
  wizardReset()
  let cfg: any = {}
  try {
    cfg = activeNode.value.configJson ? JSON.parse(String(activeNode.value.configJson)) : {}
  } catch {
    cfg = {}
  }

  if (wizard.kind === 'LLM' || wizard.kind === 'OUTPUT') {
    wizard.form.systemPrompt = cfg.systemPrompt == null ? '' : String(cfg.systemPrompt)
    wizard.form.userTemplate = cfg.userTemplate == null ? '{{input}}' : String(cfg.userTemplate)
    wizard.form.toolEnabled = cfg.toolEnabled == null ? true : Boolean(cfg.toolEnabled)
    wizard.form.allowedToolKeysJson = cfg.allowedToolKeysJson == null ? '' : String(cfg.allowedToolKeysJson)
    wizard.form.ragFromNodeKey = cfg.ragFromNodeKey == null ? '' : String(cfg.ragFromNodeKey)
    wizard.form.outputMode = cfg.outputMode == null ? 'CONTRACT' : String(cfg.outputMode)
  }
  if (wizard.kind === 'TOOL_CALL') {
    wizard.form.toolKey = cfg.toolKey == null ? '' : String(cfg.toolKey)
    wizard.form.argumentsTemplateJson = cfg.argumentsTemplateJson == null ? '{}' : String(cfg.argumentsTemplateJson)
    wizard.form.allowedToolKeysJson = cfg.allowedToolKeysJson == null ? '' : String(cfg.allowedToolKeysJson)
  }
  if (wizard.kind === 'RAG_RETRIEVE') {
    wizard.form.queryTemplate = cfg.queryTemplate == null ? '{{input}}' : String(cfg.queryTemplate)
    if (Array.isArray(cfg.ragTags)) {
      wizard.form.ragTagsJson = JSON.stringify(cfg.ragTags || [])
    } else if (cfg.ragTagsJson != null) {
      wizard.form.ragTagsJson = String(cfg.ragTagsJson)
    } else {
      wizard.form.ragTagsJson = '[]'
    }
  }
  if (wizard.kind === 'IF') {
    wizard.form.ifExpr = cfg.expr == null ? '' : String(cfg.expr)
    wizard.form.ifVarPath = cfg.varPath == null ? '' : String(cfg.varPath)
    wizard.form.ifEquals = cfg.equals == null ? '' : String(cfg.equals)
  }
}

const wizardApplyToJson = () => {
  if (!activeNode.value) return
  let cfg: any = {}
  try {
    cfg = activeNode.value.configJson ? JSON.parse(String(activeNode.value.configJson)) : {}
  } catch {
    cfg = {}
  }

  if (wizard.kind === 'LLM' || wizard.kind === 'OUTPUT') {
    cfg.systemPrompt = wizard.form.systemPrompt || ''
    cfg.userTemplate = wizard.form.userTemplate || '{{input}}'
    cfg.toolEnabled = Boolean(wizard.form.toolEnabled)
    if (wizard.form.allowedToolKeysJson && String(wizard.form.allowedToolKeysJson).trim()) {
      cfg.allowedToolKeysJson = String(wizard.form.allowedToolKeysJson).trim()
    } else {
      delete cfg.allowedToolKeysJson
    }
    if (wizard.form.ragFromNodeKey && String(wizard.form.ragFromNodeKey).trim()) {
      cfg.ragFromNodeKey = String(wizard.form.ragFromNodeKey).trim()
    } else {
      delete cfg.ragFromNodeKey
    }
    if (wizard.kind === 'OUTPUT') {
      cfg.outputMode = wizard.form.outputMode || 'CONTRACT'
    }
  }
  if (wizard.kind === 'TOOL_CALL') {
    cfg.toolKey = String(wizard.form.toolKey || '').trim()
    cfg.argumentsTemplateJson = wizard.form.argumentsTemplateJson || '{}'
    if (wizard.form.allowedToolKeysJson && String(wizard.form.allowedToolKeysJson).trim()) {
      cfg.allowedToolKeysJson = String(wizard.form.allowedToolKeysJson).trim()
    } else {
      delete cfg.allowedToolKeysJson
    }
  }
  if (wizard.kind === 'RAG_RETRIEVE') {
    cfg.queryTemplate = wizard.form.queryTemplate || '{{input}}'
    if (wizard.form.ragTagsJson && String(wizard.form.ragTagsJson).trim()) {
      try {
        const arr = JSON.parse(String(wizard.form.ragTagsJson))
        cfg.ragTags = Array.isArray(arr) ? arr : []
      } catch {
        cfg.ragTags = []
      }
    } else {
      delete cfg.ragTags
    }
  }
  if (wizard.kind === 'IF') {
    if (wizard.form.ifExpr && String(wizard.form.ifExpr).trim()) {
      cfg.expr = String(wizard.form.ifExpr).trim()
      delete cfg.varPath
      delete cfg.equals
    } else {
      delete cfg.expr
      if (wizard.form.ifVarPath && String(wizard.form.ifVarPath).trim()) {
        cfg.varPath = String(wizard.form.ifVarPath).trim()
      } else {
        delete cfg.varPath
      }
      if (wizard.form.ifEquals && String(wizard.form.ifEquals).trim()) {
        cfg.equals = String(wizard.form.ifEquals).trim()
      } else {
        delete cfg.equals
      }
    }
  }

  activeNode.value.configJson = JSON.stringify(cfg, null, 2)
  ElMessage.success('已写回 configJson')
}

const wizardFillToolAllowSelf = () => {
  const key = String(wizard.form.toolKey || '').trim()
  if (!key) {
    ElMessage.error('请先填写 toolKey')
    return
  }
  wizard.form.allowedToolKeysJson = JSON.stringify([key])
}

watch(
  () => [activeNode.value?.nodeKey, activeNode.value?.nodeType],
  () => {
    const kind = detectWizardKind(activeNode.value?.nodeType)
    wizard.kind = kind as any
    wizard.visible = Boolean(kind)
    if (wizard.visible) {
      wizardLoadFromJson()
    }
  },
  { immediate: true }
)

const stageRef = ref<HTMLDivElement | null>(null)
const stageSize = reactive({ w: 2000, h: 1400 })

const viewportStyle = computed(() => {
  return {
    transform: `translate(${view.x}px, ${view.y}px) scale(${view.zoom})`,
    transformOrigin: '0 0'
  }
})

const genNodeKey = () => {
  const s = Math.random().toString(36).slice(2, 8)
  return `n_${Date.now().toString(36)}_${s}`
}

const defaultGraph = () => {
  const startKey = 'start'
  const llmKey = 'llm_1'
  const outKey = 'output_1'
  const endKey = 'end'
  nodes.value = [
    { nodeKey: startKey, nodeType: 'START', nodeName: 'Start', configJson: '{}', positionX: 120, positionY: 160 },
    {
      nodeKey: llmKey,
      nodeType: 'LLM',
      nodeName: 'LLM',
      configJson: JSON.stringify({ userTemplate: '{{input}}', toolEnabled: true }, null, 2),
      positionX: 420,
      positionY: 160
    },
    {
      nodeKey: outKey,
      nodeType: 'OUTPUT',
      nodeName: 'Output',
      configJson: JSON.stringify({ userTemplate: '{{input}}', outputMode: 'CONTRACT' }, null, 2),
      positionX: 740,
      positionY: 160
    },
    { nodeKey: endKey, nodeType: 'END', nodeName: 'End', configJson: '{}', positionX: 1040, positionY: 160 }
  ] as any
  edges.value = [
    { sourceKey: startKey, targetKey: llmKey, edgeType: 'DEFAULT' },
    { sourceKey: llmKey, targetKey: outKey, edgeType: 'DEFAULT' },
    { sourceKey: outKey, targetKey: endKey, edgeType: 'DEFAULT' }
  ] as any
  view.zoom = 1
  view.x = 0
  view.y = 0
  selected.kind = 'none'
  selected.key = ''
}

const load = async () => {
  if (!workflowId.value) {
    ElMessage.error('缺少 workflowId（请从列表页进入）')
    return
  }
  if (!workflowVersionId.value) {
    ElMessage.error('缺少 workflowVersionId（请从版本页选择一个版本进入编辑器）')
    return
  }

  const w = await getWorkflow(workflowId.value)
  wf.value = w.data
  const list = await listWorkflowVersions({ workflowId: workflowId.value })
  version.value = (list.data || []).find(v => v.id === workflowVersionId.value) || null

  const graphRaw = version.value?.graphJson
  if (!graphRaw) {
    defaultGraph()
    return
  }
  try {
    const parsed = JSON.parse(graphRaw)
    nodes.value = (parsed?.nodes || []).map((n: any) => ({
      nodeKey: String(n.nodeKey || genNodeKey()),
      nodeType: String(n.nodeType || 'LLM'),
      nodeName: String(n.nodeName || ''),
      configJson: typeof n.configJson === 'string' ? n.configJson : JSON.stringify(n.configJson || {}, null, 2),
      positionX: Number.isFinite(n.positionX) ? n.positionX : 100,
      positionY: Number.isFinite(n.positionY) ? n.positionY : 100
    }))
    edges.value = (parsed?.edges || []).map((e: any) => ({
      sourceKey: String(e.sourceKey || ''),
      targetKey: String(e.targetKey || ''),
      edgeType: String(e.edgeType || 'DEFAULT'),
      conditionExpr: e.conditionExpr == null ? undefined : String(e.conditionExpr)
    }))
    if (parsed?.viewport) {
      view.zoom = Number(parsed.viewport.zoom || 1) || 1
      view.x = Number(parsed.viewport.x || 0) || 0
      view.y = Number(parsed.viewport.y || 0) || 0
    }
    selected.kind = 'none'
    selected.key = ''
  } catch (e: any) {
    console.error(e)
    ElMessage.error('graphJson 解析失败，已加载默认图')
    defaultGraph()
  }
}

const saving = ref(false)

const validateGraph = () => {
  const startCount = nodes.value.filter(n => n.nodeType === 'START').length
  if (startCount !== 1) {
    ElMessage.error(`START 节点数量必须为 1（当前=${startCount}）`)
    return false
  }
  for (const n of nodes.value) {
    const t = String(n.nodeType || '').toUpperCase()
    if (!t) {
      ElMessage.error(`存在空 nodeType：${n.nodeKey}`)
      return false
    }
    // 对“需要配置 JSON 的节点”做轻校验（避免运行时直接炸）
    if (t === 'LLM' || t === 'OUTPUT' || t === 'TOOL_CALL' || t === 'RAG_RETRIEVE' || t === 'IF') {
      if (n.configJson && String(n.configJson).trim()) {
        try {
          JSON.parse(String(n.configJson))
        } catch {
          ElMessage.error(`节点 configJson 非法 JSON：${n.nodeKey}`)
          return false
        }
      }
    }
  }
  for (const e of edges.value) {
    if (!e.sourceKey || !e.targetKey) {
      ElMessage.error('存在空 source/target 的边')
      return false
    }
    if (!nodes.value.some(n => n.nodeKey === e.sourceKey) || !nodes.value.some(n => n.nodeKey === e.targetKey)) {
      ElMessage.error(`边引用了不存在的节点：${e.sourceKey} -> ${e.targetKey}`)
      return false
    }
  }
  return true
}

const save = async () => {
  if (!workflowVersionId.value) {
    ElMessage.error('缺少 workflowVersionId')
    return
  }
  if (!validateGraph()) return

  const graphJson = JSON.stringify(
    {
      viewport: { zoom: view.zoom, x: view.x, y: view.y },
      nodes: nodes.value,
      edges: edges.value
    },
    null,
    2
  )

  saving.value = true
  try {
    await saveWorkflowGraph({
      workflowVersionId: workflowVersionId.value,
      graphJson,
      defaultConfigJson: version.value?.defaultConfigJson || undefined,
      nodes: nodes.value.map(n => ({
        nodeKey: n.nodeKey,
        nodeType: n.nodeType,
        nodeName: n.nodeName || undefined,
        configJson: n.configJson || undefined,
        positionX: n.positionX,
        positionY: n.positionY
      })),
      edges: edges.value.map(e => ({
        sourceKey: e.sourceKey,
        targetKey: e.targetKey,
        edgeType: e.edgeType || 'DEFAULT',
        conditionExpr: e.conditionExpr || undefined
      }))
    })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

const addNode = (type: string) => {
  const key = genNodeKey()
  const centerX = 260 + Math.round((Math.random() - 0.5) * 60)
  const centerY = 220 + Math.round((Math.random() - 0.5) * 60)
  nodes.value.push({
    nodeKey: key,
    nodeType: type,
    nodeName: type,
    configJson: '{}',
    positionX: centerX,
    positionY: centerY
  } as any)
  selected.kind = 'node'
  selected.key = key
}

const removeNode = (nodeKey: string) => {
  nodes.value = nodes.value.filter(n => n.nodeKey !== nodeKey)
  edges.value = edges.value.filter(e => e.sourceKey !== nodeKey && e.targetKey !== nodeKey)
  selected.kind = 'none'
  selected.key = ''
}

const edgeKey = (e: { sourceKey: string; targetKey: string; edgeType?: string }) => `${e.sourceKey}->${e.targetKey}@${e.edgeType || 'DEFAULT'}`

const selectEdge = (e: EdgeVM) => {
  if (mode.connect) return
  selected.kind = 'edge'
  selected.key = edgeKey(e)
}
const removeEdge = (e: EdgeVM) => {
  edges.value = edges.value.filter(x => edgeKey(x) !== edgeKey(e))
  selected.kind = 'none'
  selected.key = ''
}

const onNodeClick = (n: NodeVM) => {
  if (mode.connect) {
    if (!connectState.sourceKey) {
      connectState.sourceKey = n.nodeKey
      return
    }
    const source = connectState.sourceKey
    const target = n.nodeKey
    connectState.sourceKey = ''
    if (source === target) {
      ElMessage.warning('不能连到自己')
      return
    }
    const existed = edges.value.some(e => e.sourceKey === source && e.targetKey === target)
    if (existed) {
      ElMessage.warning('该连线已存在')
      return
    }
    edges.value.push({ sourceKey: source, targetKey: target, edgeType: 'DEFAULT' } as any)
    return
  }
  selected.kind = 'node'
  selected.key = n.nodeKey
}

const nodeStyle = (n: NodeVM) => {
  return {
    transform: `translate(${n.positionX}px, ${n.positionY}px)`
  }
}

const nodeCenter = (key: string) => {
  const n = nodes.value.find(x => x.nodeKey === key)
  if (!n) return { x: 0, y: 0 }
  return { x: n.positionX + 120, y: n.positionY + 34 }
}

const edgePath = (e: EdgeVM) => {
  const s = nodeCenter(e.sourceKey)
  const t = nodeCenter(e.targetKey)
  const dx = Math.max(60, Math.abs(t.x - s.x) * 0.4)
  const c1 = { x: s.x + dx, y: s.y }
  const c2 = { x: t.x - dx, y: t.y }
  return `M ${s.x} ${s.y} C ${c1.x} ${c1.y}, ${c2.x} ${c2.y}, ${t.x} ${t.y}`
}

// Drag logic
const drag = reactive<{
  kind: 'none' | 'node' | 'pan'
  nodeKey: string
  startX: number
  startY: number
  baseX: number
  baseY: number
  pointerId: number | null
}>({ kind: 'none', nodeKey: '', startX: 0, startY: 0, baseX: 0, baseY: 0, pointerId: null })

const onNodePointerDown = (ev: PointerEvent, n: NodeVM) => {
  if (mode.connect) return
  if (mode.pan) return
  ;(ev.currentTarget as HTMLElement).setPointerCapture(ev.pointerId)
  drag.kind = 'node'
  drag.nodeKey = n.nodeKey
  drag.startX = ev.clientX
  drag.startY = ev.clientY
  drag.baseX = n.positionX
  drag.baseY = n.positionY
  drag.pointerId = ev.pointerId
  selected.kind = 'node'
  selected.key = n.nodeKey
}

const onStagePointerDown = (ev: PointerEvent) => {
  if (!mode.pan) {
    if (!mode.connect) {
      selected.kind = 'none'
      selected.key = ''
    }
    return
  }
  if (!stageRef.value) return
  stageRef.value.setPointerCapture(ev.pointerId)
  drag.kind = 'pan'
  drag.nodeKey = ''
  drag.startX = ev.clientX
  drag.startY = ev.clientY
  drag.baseX = view.x
  drag.baseY = view.y
  drag.pointerId = ev.pointerId
}

const onStagePointerMove = (ev: PointerEvent) => {
  if (drag.pointerId == null || ev.pointerId !== drag.pointerId) return
  const dx = ev.clientX - drag.startX
  const dy = ev.clientY - drag.startY
  if (drag.kind === 'pan') {
    view.x = drag.baseX + dx
    view.y = drag.baseY + dy
    return
  }
  if (drag.kind === 'node') {
    const n = nodes.value.find(x => x.nodeKey === drag.nodeKey)
    if (!n) return
    // 屏幕位移 -> 画布位移，需要除以 zoom
    n.positionX = Math.round(drag.baseX + dx / view.zoom)
    n.positionY = Math.round(drag.baseY + dy / view.zoom)
  }
}

const onStagePointerUp = (ev: PointerEvent) => {
  if (drag.pointerId == null || ev.pointerId !== drag.pointerId) return
  drag.kind = 'none'
  drag.nodeKey = ''
  drag.pointerId = null
}

const zoomIn = () => {
  view.zoom = Math.min(2.5, Math.round((view.zoom + 0.1) * 10) / 10)
}
const zoomOut = () => {
  view.zoom = Math.max(0.4, Math.round((view.zoom - 0.1) * 10) / 10)
}
const resetView = () => {
  view.zoom = 1
  view.x = 0
  view.y = 0
}

const formatNodeJson = (n: NodeVM) => {
  try {
    const obj = JSON.parse(n.configJson || '{}')
    n.configJson = JSON.stringify(obj, null, 2)
    ElMessage.success('已格式化')
  } catch {
    ElMessage.error('configJson 不是合法 JSON')
  }
}
const validateNodeJson = (n: NodeVM) => {
  try {
    JSON.parse(n.configJson || '{}')
    ElMessage.success('JSON 合法')
  } catch {
    ElMessage.error('JSON 非法')
  }
}

load()
</script>

<style scoped lang="scss">
.editor-shell {
  display: grid;
  grid-template-columns: 220px 1fr 320px;
  gap: 12px;
  padding: 12px;
  min-height: calc(100vh - 150px);
}
.left-panel,
.right-panel {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px;
  overflow: auto;
}
.panel-title {
  font-weight: 700;
  margin-bottom: 10px;
}
.palette {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.palette-btn {
  width: 100%;
  justify-content: flex-start;
}
.tools {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tool-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.zoom-label {
  width: 74px;
  text-align: center;
  color: var(--gemini-text-secondary);
}
.hint {
  color: var(--gemini-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.stage-wrap {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  overflow: hidden;
}
.stage {
  position: relative;
  width: 100%;
  height: calc(100vh - 190px);
  overflow: hidden;
  user-select: none;
}
.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  opacity: 0.6;
}
.viewport {
  position: absolute;
  left: 0;
  top: 0;
  width: 2400px;
  height: 1600px;
}
.edges {
  position: absolute;
  left: 0;
  top: 0;
  width: 2400px;
  height: 1600px;
  pointer-events: none;
}
.edge-path {
  pointer-events: stroke;
  fill: none;
  stroke: rgba(122, 162, 255, 0.55);
  stroke-width: 2;
}
.edge-path.selected {
  stroke: rgba(255, 204, 102, 0.95);
  stroke-width: 3;
}
.node {
  position: absolute;
  width: 240px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  cursor: grab;
}
.node.selected {
  border-color: rgba(255, 204, 102, 0.95);
  box-shadow: 0 0 0 2px rgba(255, 204, 102, 0.2);
}
.node.start {
  border-color: rgba(124, 255, 192, 0.55);
}
.node.end {
  border-color: rgba(255, 122, 162, 0.45);
}
.node-hd {
  padding: 10px 10px 8px;
}
.node-type {
  font-size: 11px;
  color: var(--gemini-text-secondary);
  letter-spacing: 0.4px;
}
.node-name {
  font-size: 13px;
  font-weight: 750;
  margin-top: 4px;
  color: var(--gemini-text-primary);
}
.node-ft {
  padding: 8px 10px 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.node-key {
  font-size: 11px;
  color: var(--gemini-text-secondary);
}
.inspector-row {
  margin-bottom: 10px;
}
.lbl {
  color: var(--gemini-text-secondary);
  font-size: 12px;
  margin-bottom: 6px;
}
.val {
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}
.cfg-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.wizard {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 10px;
}
.wizard-form :deep(.el-form-item) {
  margin-bottom: 10px;
}
.muted {
  color: var(--gemini-text-secondary);
}
.bind-row {
  display: flex;
  align-items: center;
}
.bind-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bind-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}
.bind-title {
  font-weight: 600;
}
.bind-sub {
  font-size: 12px;
}
.bind-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
