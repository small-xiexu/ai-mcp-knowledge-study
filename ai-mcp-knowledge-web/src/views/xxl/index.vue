<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">任务中心</h2>
        <p class="subtitle">统一管理调度任务与执行日志</p>
      </div>
      <div class="header-actions">
        <el-button 
          class="gemini-btn-secondary"
          @click="reloadJobs(true)"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button
          type="primary"
          class="gemini-btn-primary"
          @click="openCreateDialog"
        >
          <el-icon><Plus /></el-icon>
          新建任务
        </el-button>
      </div>
    </div>

    <div class="gemini-card tabs-container">
      <el-tabs
        v-model="activeTab"
        class="gemini-pill-tabs"
      >
        <el-tab-pane
          label="任务列表"
          name="jobs"
        >
          <el-table
            v-loading="jobLoading"
            :data="jobTable"
            class="gemini-table"
            style="width: 100%"
          >
            <el-table-column
              prop="id"
              label="ID"
              width="70"
            />
            <el-table-column
              prop="jobDesc"
              label="任务描述"
              min-width="200"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span style="color: var(--gemini-text-primary); font-weight: 500;">{{ row.jobDesc }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="executorHandler"
              label="Handler"
              min-width="160"
              show-overflow-tooltip
            >
               <template #default="{ row }">
                <span style="font-family: monospace; color: var(--gemini-accent);">{{ row.executorHandler }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="scheduleConf"
              label="CRON"
              min-width="150"
              show-overflow-tooltip
            >
               <template #default="{ row }">
                <el-tag size="small" effect="dark" type="info" style="background: rgba(255,255,255,0.1); border: none;">
                  {{ row.scheduleConf }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="triggerStatus"
              label="状态"
              width="120"
            >
              <template #default="{ row }">
                <div class="status-indicator-v2" :class="{ active: row.triggerStatus === 1 }">
                  <div class="status-dot"></div>
                  <span class="status-text">{{ row.triggerStatus === 1 ? '正在运行' : '暂停中' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              prop="author"
              label="创建人"
              width="120"
            />
            <el-table-column
              label="更新时间"
              width="180"
            >
              <template #default="{ row }">
                {{ formatDateTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="160"
              fixed="right"
              align="right"
            >
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button
                    link
                    type="primary"
                    class="action-btn"
                    @click="openEditDialog(row)"
                  >
                    <el-icon><EditPen /></el-icon>
                  </el-button>
                  <el-button
                    link
                    class="action-btn"
                    :class="row.triggerStatus === 1 ? 'warning' : 'success'"
                    @click="toggleJob(row)"
                  >
                    <el-icon v-if="row.triggerStatus === 1"><VideoPause /></el-icon>
                    <el-icon v-else><VideoPlay /></el-icon>
                  </el-button>
                  <el-dropdown
                    trigger="click"
                    popper-class="gemini-dropdown"
                  >
                    <el-button
                      link
                      class="action-btn"
                    >
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="openDetail(row)">
                          <el-icon><Document /></el-icon>详情
                        </el-dropdown-item>
                        <el-dropdown-item @click="openTriggerDialog(row)">
                          <el-icon><Lightning /></el-icon>触发一次
                        </el-dropdown-item>
                        <el-dropdown-item
                          divided
                          style="color: var(--gemini-danger)"
                          @click="confirmRemove(row)"
                        >
                          <el-icon><Delete /></el-icon>删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="jobPagination.pageNum"
              v-model:page-size="jobPagination.pageSize"
              :total="jobPagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              class="gemini-pagination"
              @size-change="handleJobSizeChange"
              @current-change="handleJobPageChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane
          label="日志查询"
          name="logs"
        >
          <div class="log-toolbar">
             <el-form
              :inline="true"
              @submit.prevent
            >
              <el-form-item label="任务">
                <el-select
                  v-model="logFilters.jobId"
                  placeholder="请选择任务"
                  style="width: 240px"
                  filterable
                  clearable
                  :loading="jobSelectLoading"
                  class="gemini-select"
                  popper-class="gemini-select-dropdown"
                >
                  <el-option
                    v-for="job in jobOptions"
                    :key="job.id"
                    :label="job.jobDesc"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="logFilters.timeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="gemini-date-picker"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  class="gemini-btn-primary"
                  @click="reloadLogs"
                >
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
                <el-button class="gemini-btn-secondary" @click="resetLogFilters">
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <el-table
            v-loading="logLoading"
            :data="logTable"
            class="gemini-table"
            style="width: 100%"
          >
            <el-table-column
              prop="id"
              label="日志ID"
              width="100"
            />
             <el-table-column
              label="触发时间"
              width="180"
            >
              <template #default="{ row }">
                {{ formatDateTime(row.triggerTime) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="executorHandler"
              label="Handler"
              min-width="160"
              show-overflow-tooltip
            >
               <template #default="{ row }">
                <span style="font-family: monospace; color: var(--gemini-accent);">{{ row.executorHandler }}</span>
              </template>
            </el-table-column>
           
            <el-table-column
              label="执行结果"
              width="120"
            >
              <template #default="{ row }">
                <span :style="{ color: row.triggerCode === 200 ? 'var(--gemini-success)' : 'var(--gemini-danger)' }">
                  {{ row.triggerCode === 200 ? '成功' : '失败' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="100"
              fixed="right"
              align="right"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="openLogDetail(row)"
                >
                  查看日志
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="logPagination.pageNum"
              v-model:page-size="logPagination.pageSize"
              :total="logPagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              class="gemini-pagination"
              @size-change="handleLogSizeChange"
              @current-change="handleLogPageChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 弹窗部分 -->
    <el-dialog
      v-model="jobDialogVisible"
      :title="jobDialogTitle"
      width="720px"
      class="gemini-dialog"
      align-center
    >
      <el-form
        ref="jobFormRef"
        :model="jobForm"
        :rules="jobRules"
        label-width="120px"
      >
        <el-form-item
          label="任务描述"
          prop="jobDesc"
        >
          <el-input
            v-model="jobForm.jobDesc"
            placeholder="请输入任务描述"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item
          label="创建人"
          prop="author"
        >
          <el-input
            v-model="jobForm.author"
            placeholder="请输入创建人"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="报警邮箱">
          <el-input
            v-model="jobForm.alarmEmail"
            placeholder="可选"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="调度类型">
          <el-select
            v-model="jobForm.scheduleType"
            style="width: 100%"
            class="gemini-select"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              label="CRON"
              value="CRON"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="CRON"
          prop="scheduleConf"
        >
          <el-input
            v-model="jobForm.scheduleConf"
            placeholder="请输入 CRON 表达式"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="过期策略">
          <el-select
            v-model="jobForm.misfireStrategy"
            style="width: 100%"
            class="gemini-select"
            popper-class="gemini-select-dropdown"
          >
            <el-option
              label="忽略"
              value="DO_NOTHING"
            />
            <el-option
              label="立即执行一次"
              value="FIRE_ONCE_NOW"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="路由策略">
          <el-select
            v-model="jobForm.executorRouteStrategy"
            style="width: 100%"
            class="gemini-select"
             popper-class="gemini-select-dropdown"
          >
            <el-option
              label="FIRST"
              value="FIRST"
            />
            <el-option
              label="ROUND"
              value="ROUND"
            />
            <el-option
              label="RANDOM"
              value="RANDOM"
            />
            <el-option
              label="CONSISTENT_HASH"
              value="CONSISTENT_HASH"
            />
             <el-option
              label="FAILOVER"
              value="FAILOVER"
            />
            <el-option
              label="BUSYOVER"
              value="BUSYOVER"
            />
            <el-option
              label="SHARDING_BROADCAST"
              value="SHARDING_BROADCAST"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="Handler"
          prop="executorHandler"
        >
          <el-input
            v-model="jobForm.executorHandler"
            placeholder="请输入执行器 Handler"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="执行参数">
          <el-input
            v-model="jobForm.executorParam"
            placeholder="可选"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="阻塞策略">
          <el-select
            v-model="jobForm.executorBlockStrategy"
            style="width: 100%"
            class="gemini-select"
             popper-class="gemini-select-dropdown"
          >
            <el-option
              label="SERIAL_EXECUTION"
              value="SERIAL_EXECUTION"
            />
            <el-option
              label="DISCARD_LATER"
              value="DISCARD_LATER"
            />
            <el-option
              label="COVER_EARLY"
              value="COVER_EARLY"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number
            v-model="jobForm.executorTimeout"
            :min="0"
            :max="3600"
            class="gemini-input-number"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="失败重试">
          <el-input-number
            v-model="jobForm.executorFailRetryCount"
            :min="0"
            :max="10"
            class="gemini-input-number"
            controls-position="right"
             style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="Glue 类型">
           <el-input
            v-model="jobForm.glueType"
            disabled
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="子任务ID">
          <el-input
            v-model="jobForm.childJobId"
            placeholder="可选，多个用逗号分隔"
            class="gemini-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="jobDialogVisible = false" text class="cancel-btn">
            取消
          </el-button>
          <el-button
            type="primary"
            class="gemini-btn-primary"
            :loading="jobSaving"
            @click="submitJobForm"
          >
            提交
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="任务详情"
      width="720px"
      class="gemini-dialog"
      align-center
    >
      <el-descriptions
        v-if="jobDetail"
        :column="2"
        border
        class="gemini-descriptions"
      >
        <el-descriptions-item label="任务ID">
          {{ jobDetail.id }}
        </el-descriptions-item>
        <el-descriptions-item label="执行器">
          {{ jobDetail.jobGroup ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          {{ jobDetail.jobDesc }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人">
          {{ jobDetail.author }}
        </el-descriptions-item>
        <el-descriptions-item label="CRON">
          {{ jobDetail.scheduleConf }}
        </el-descriptions-item>
        <el-descriptions-item label="路由策略">
          {{ jobDetail.executorRouteStrategy }}
        </el-descriptions-item>
        <el-descriptions-item label="阻塞策略">
          {{ jobDetail.executorBlockStrategy }}
        </el-descriptions-item>
        <el-descriptions-item label="超时">
          {{ jobDetail.executorTimeout ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="失败重试">
          {{ jobDetail.executorFailRetryCount ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="触发状态">
          {{ jobDetail.triggerStatus === 1 ? '运行中' : '已停止' }}
        </el-descriptions-item>
        <el-descriptions-item label="子任务">
          {{ jobDetail.childJobId || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button
          type="primary"
          class="gemini-btn-primary"
          @click="detailDialogVisible = false"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="triggerDialogVisible"
      title="手动触发"
      width="520px"
      class="gemini-dialog"
      align-center
    >
      <el-form
        :model="triggerForm"
        label-width="100px"
      >
        <el-form-item label="执行参数">
          <el-input
            v-model="triggerForm.executorParam"
            placeholder="可选"
            class="gemini-input"
          />
        </el-form-item>
        <el-form-item label="指定机器">
          <el-input
            v-model="triggerForm.addressList"
            placeholder="可选，逗号分隔"
            class="gemini-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
         <div class="dialog-footer">
          <el-button @click="triggerDialogVisible = false" text class="cancel-btn">
            取消
          </el-button>
          <el-button
            type="primary"
            class="gemini-btn-primary"
            :loading="triggerLoading"
            @click="submitTrigger"
          >
            触发
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      v-model="logDrawerVisible"
      title="日志详情"
      size="50%"
      class="gemini-drawer"
    >
      <div class="log-detail">
        <div class="log-toolbar">
          <el-button
            size="small"
            class="gemini-btn-secondary"
            @click="reloadLogDetail"
          >
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button
             class="gemini-btn-primary"
            size="small"
            type="primary"
            :disabled="logDetailEnd"
            @click="loadMoreLog"
          >
            <el-icon><Download /></el-icon> 加载更多
          </el-button>
        </div>
        <el-input
          v-model="logDetailContent"
          type="textarea"
          :rows="20"
          readonly
          class="gemini-input log-textarea"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createXxlJob,
  getXxlJobDetail,
  getXxlJobList,
  getXxlJobOptions,
  getXxlJobLogDetail,
  getXxlJobLogList,
  removeXxlJob,
  startXxlJob,
  stopXxlJob,
  triggerXxlJob,
  updateXxlJob
} from '@/api/xxl'
import type {
  XxlJob,
  XxlJobCreateRequest,
  XxlJobDetail,
  XxlJobLog,
  XxlJobLogListRequest,
  XxlJobTriggerRequest,
  XxlJobUpdateRequest
} from '@/types/entity'
import type { FormInstance, FormRules } from 'element-plus'

const activeTab = ref('jobs')
const jobLoading = ref(false)
const logLoading = ref(false)
const jobSaving = ref(false)
const triggerLoading = ref(false)

const jobTable = ref<XxlJob[]>([])
const jobPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const jobDialogVisible = ref(false)
const jobDialogTitle = ref('新建任务')
const jobFormRef = ref<FormInstance>()
const jobForm = reactive<XxlJobUpdateRequest>({
  id: 0,
  jobDesc: '',
  author: '',
  alarmEmail: '',
  scheduleType: 'CRON',
  scheduleConf: '',
  misfireStrategy: 'DO_NOTHING',
  executorRouteStrategy: 'FIRST',
  executorHandler: '',
  executorParam: '',
  executorBlockStrategy: 'SERIAL_EXECUTION',
  executorTimeout: 0,
  executorFailRetryCount: 0,
  glueType: 'BEAN',
  childJobId: ''
})

const jobRules: FormRules = {
  jobDesc: [{ required: true, message: '请输入任务描述', trigger: 'blur' }],
  author: [{ required: true, message: '请输入创建人', trigger: 'blur' }],
  scheduleConf: [{ required: true, message: '请输入 CRON', trigger: 'blur' }],
  executorHandler: [{ required: true, message: '请输入 Handler', trigger: 'blur' }]
}

const detailDialogVisible = ref(false)
const jobDetail = ref<XxlJobDetail | null>(null)

const triggerDialogVisible = ref(false)
const triggerForm = reactive<XxlJobTriggerRequest>({
  id: 0,
  executorParam: '',
  addressList: ''
})

const logTable = ref<XxlJobLog[]>([])
const logPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const logFilters = reactive<{
  jobId: number | null
  timeRange: string[] | null
}>({
  jobId: null,
  timeRange: null
})

const jobOptions = ref<XxlJob[]>([])
const jobSelectLoading = ref(false)

const logDrawerVisible = ref(false)
const currentLogId = ref<number | null>(null)
const logDetailContent = ref('')
const logDetailFromLine = ref(0)
const logDetailEnd = ref(false)

const reloadJobs = async (forceRefresh = false) => {
  jobLoading.value = true
  try {
    if (forceRefresh) {
      await loadAllJobs(true)
    }
    const res = await getXxlJobList({
      pageNum: jobPagination.pageNum,
      pageSize: jobPagination.pageSize
    })
    jobTable.value = res.data.records
    jobPagination.total = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务失败')
  } finally {
    jobLoading.value = false
  }
}

const handleJobSizeChange = () => {
  jobPagination.pageNum = 1
  reloadJobs()
}

const handleJobPageChange = () => {
  reloadJobs()
}

const openCreateDialog = () => {
  jobDialogTitle.value = '新建任务'
  Object.assign(jobForm, {
    id: 0,
    jobDesc: '',
    author: '',
    alarmEmail: '',
    scheduleType: 'CRON',
    scheduleConf: '',
    misfireStrategy: 'DO_NOTHING',
    executorRouteStrategy: 'FIRST',
    executorHandler: '',
    executorParam: '',
    executorBlockStrategy: 'SERIAL_EXECUTION',
    executorTimeout: 0,
    executorFailRetryCount: 0,
    glueType: 'BEAN',
    childJobId: ''
  })
  jobDialogVisible.value = true
}

const openEditDialog = async (row: XxlJob) => {
  jobDialogTitle.value = '编辑任务'
  try {
    const res = await getXxlJobDetail(row.id)
    const detail = res.data
    Object.assign(jobForm, {
      id: detail.id,
      jobDesc: detail.jobDesc,
      author: detail.author || '',
      alarmEmail: detail.alarmEmail || '',
      scheduleType: detail.scheduleType || 'CRON',
      scheduleConf: detail.scheduleConf || '',
      misfireStrategy: detail.misfireStrategy || 'DO_NOTHING',
      executorRouteStrategy: detail.executorRouteStrategy || 'FIRST',
      executorHandler: detail.executorHandler || '',
      executorParam: detail.executorParam || '',
      executorBlockStrategy: detail.executorBlockStrategy || 'SERIAL_EXECUTION',
      executorTimeout: detail.executorTimeout || 0,
      executorFailRetryCount: detail.executorFailRetryCount || 0,
      glueType: detail.glueType || 'BEAN',
      childJobId: detail.childJobId || ''
    })
    jobDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务详情失败')
  }
}

const submitJobForm = async () => {
  if (!jobFormRef.value) return
  await jobFormRef.value.validate(async (valid) => {
    if (!valid) return
    jobSaving.value = true
    try {
      if (jobForm.id) {
        await updateXxlJob(jobForm)
        ElMessage.success('任务更新成功')
      } else {
        const payload: XxlJobCreateRequest = { ...jobForm }
        await createXxlJob(payload)
        ElMessage.success('任务创建成功')
      }
      jobDialogVisible.value = false
      reloadJobs()
    } catch (error: any) {
      ElMessage.error(error.message || '提交失败')
    } finally {
      jobSaving.value = false
    }
  })
}

const openDetail = async (row: XxlJob) => {
  try {
    const res = await getXxlJobDetail(row.id)
    jobDetail.value = res.data
    detailDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取详情失败')
  }
}

const toggleJob = async (row: XxlJob) => {
  try {
    if (row.triggerStatus === 1) {
      await stopXxlJob(row.id)
      ElMessage.success('任务已停止')
    } else {
      await startXxlJob(row.id)
      ElMessage.success('任务已启动')
    }
    reloadJobs()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const openTriggerDialog = (row: XxlJob) => {
  triggerForm.id = row.id
  triggerForm.executorParam = row.executorParam || ''
  triggerForm.addressList = ''
  triggerDialogVisible.value = true
}

const submitTrigger = async () => {
  triggerLoading.value = true
  try {
    const res = await triggerXxlJob(triggerForm)
    const message = res.message || '触发成功'
    ElMessage.success(message)
    triggerDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '触发失败')
  } finally {
    triggerLoading.value = false
  }
}

const confirmRemove = async (row: XxlJob) => {
  try {
    await ElMessageBox.confirm(`确定删除任务 "${row.jobDesc}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeXxlJob(row.id)
    ElMessage.success('删除成功')
    reloadJobs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const reloadLogs = async () => {
  const jobId = logFilters.jobId
  if (!jobId) {
    ElMessage.warning('请选择任务')
    return
  }
  logLoading.value = true
  const request: XxlJobLogListRequest = {
    jobId,
    pageNum: logPagination.pageNum,
    pageSize: logPagination.pageSize
  }
  if (logFilters.timeRange && logFilters.timeRange.length === 2) {
    request.startTime = logFilters.timeRange[0]
    request.endTime = logFilters.timeRange[1]
  }
  try {
    const res = await getXxlJobLogList(request)
    logTable.value = res.data.records
    logPagination.total = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载日志失败')
  } finally {
    logLoading.value = false
  }
}

const handleLogSizeChange = () => {
  logPagination.pageNum = 1
  reloadLogs()
}

const handleLogPageChange = () => {
  reloadLogs()
}

const resetLogFilters = () => {
  logFilters.jobId = null
  logFilters.timeRange = null
  logTable.value = []
  logPagination.pageNum = 1
  logPagination.total = 0
}

const openLogDetail = async (row: XxlJobLog) => {
  currentLogId.value = row.id
  logDetailContent.value = ''
  logDetailFromLine.value = 0
  logDetailEnd.value = false
  logDrawerVisible.value = true
  await loadLogDetail(true)
}

const reloadLogDetail = async () => {
  if (!currentLogId.value) return
  logDetailContent.value = ''
  logDetailFromLine.value = 0
  logDetailEnd.value = false
  await loadLogDetail(true)
}

const loadMoreLog = async () => {
  await loadLogDetail(false)
}

const loadLogDetail = async (reset: boolean) => {
  if (!currentLogId.value) return
  try {
    const res = await getXxlJobLogDetail(currentLogId.value, logDetailFromLine.value)
    const detail = res.data
    if (reset) {
      logDetailContent.value = detail.logContent || ''
    } else {
      logDetailContent.value += detail.logContent || ''
    }
    logDetailFromLine.value = detail.toLineNum || logDetailFromLine.value
    logDetailEnd.value = Boolean(detail.end)
  } catch (error: any) {
    ElMessage.error(error.message || '加载日志详情失败')
  }
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const normalized = value.replace('T', ' ')
  const noMillis = normalized.includes('.') ? normalized.split('.')[0] : normalized
  if (noMillis.includes('+')) {
    return noMillis.split('+')[0]
  }
  if (noMillis.endsWith('Z')) {
    return noMillis.slice(0, -1)
  }
  return noMillis
}



onMounted(() => {
  loadAllJobs()
  reloadJobs()
})

const loadAllJobs = async (forceRefresh = false) => {
  jobSelectLoading.value = true
  try {
    const res = await getXxlJobOptions(forceRefresh)
    jobOptions.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务列表失败')
  } finally {
    jobSelectLoading.value = false
  }
}
</script>

<style scoped>

/* Gemini Pill Tabs Layout */
.tabs-container {
  padding: 12px 0 0;
}

:deep(.gemini-pill-tabs) {
  border: none;
}

:deep(.gemini-pill-tabs .el-tabs__header) {
  margin: 0 0 20px 24px;
  border-bottom: none;
}

:deep(.gemini-pill-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.gemini-pill-tabs .el-tabs__active-bar) {
  display: none;
}

:deep(.gemini-pill-tabs .el-tabs__item) {
  height: 34px;
  line-height: 32px;
  padding: 0 20px !important;
  border-radius: 999px;
  margin-right: 12px;
  color: var(--gemini-text-secondary);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
  font-size: 14px;
}

:deep(.gemini-pill-tabs .el-tabs__item.is-active) {
  background: rgba(138, 180, 248, 0.12);
  color: #8ab4f8;
  border-color: rgba(138, 180, 248, 0.3);
  font-weight: 600;
}

:deep(.gemini-pill-tabs .el-tabs__item:hover:not(.is-active)) {
  color: var(--gemini-text-primary);
  background: rgba(255, 255, 255, 0.05);
}

/* V2 Status Indicator with Glow */
.status-indicator-v2 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--gemini-text-secondary);
  position: relative;
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--gemini-text-secondary);
}

.status-indicator-v2.active {
  background: rgba(129, 201, 149, 0.08);
  border-color: rgba(129, 201, 149, 0.2);
}

.status-indicator-v2.active .status-dot {
  background-color: #81c995;
  box-shadow: 0 0 10px rgba(129, 201, 149, 0.6);
}

.status-indicator-v2.active .status-dot::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #81c995;
  animation: pulse-glow 2s infinite;
}

.status-indicator-v2.active .status-text {
  color: #81c995;
}

@keyframes pulse-glow {
  0% { transform: scale(1); opacity: 0.8; }
  70% { transform: scale(2.5); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

/* Table Enhancements */
:deep(.gemini-table) {
  padding: 0 12px;
}

:deep(.gemini-table th.el-table__cell) {
  padding: 12px 0 !important;
  background-color: rgba(255, 255, 255, 0.02) !important;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--gemini-text-secondary);
}

:deep(.gemini-table td.el-table__cell) {
  padding: 14px 0 !important;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.action-btn {
  padding: 4px 10px;
  height: 32px;
  border-radius: 8px;
  color: var(--gemini-text-secondary);
  transition: all 0.2s;
  
  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: var(--gemini-text-primary);
  }
}

.pagination-container {
  margin-top: 24px;
  padding: 0 24px 24px;
  display: flex;
  justify-content: flex-end;
}

.log-toolbar {
  padding: 0 24px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.log-textarea :deep(.el-textarea__inner) {
  background-color: #0f1012 !important;
  color: #a8b3cf;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 24px;
  border: 1px solid var(--gemini-border);
  box-shadow: none;
  
  &:focus {
    border-color: var(--gemini-accent);
  }
}
</style>

