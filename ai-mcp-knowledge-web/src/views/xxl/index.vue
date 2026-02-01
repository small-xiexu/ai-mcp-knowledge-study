<template>
  <div class="xxl-admin">
    <el-card class="xxl-card">
      <div class="page-header">
        <div>
          <h2>任务中心</h2>
          <p>统一管理调度任务与执行日志</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="reloadJobs(true)">
            <el-icon><Refresh /></el-icon>
            刷新任务
          </el-button>
          <el-button type="success" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </div>

      <el-tabs v-model="activeTab" type="card">
        <el-tab-pane label="任务列表" name="jobs">
          <el-table v-loading="jobLoading" :data="jobTable" border stripe>
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="jobDesc" label="任务描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="executorHandler" label="Handler" min-width="160" show-overflow-tooltip />
            <el-table-column prop="scheduleConf" label="CRON" min-width="180" show-overflow-tooltip />
            <el-table-column prop="executorRouteStrategy" label="路由策略" width="140" />
            <el-table-column prop="triggerStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.triggerStatus === 1 ? 'success' : 'info'">
                  {{ row.triggerStatus === 1 ? '运行中' : '已停止' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="author" label="创建人" width="120" />
            <el-table-column label="更新时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="360" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openDetail(row)">详情</el-button>
                <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  size="small"
                  :type="row.triggerStatus === 1 ? 'warning' : 'success'"
                  @click="toggleJob(row)"
                >
                  {{ row.triggerStatus === 1 ? '停止' : '启动' }}
                </el-button>
                <el-button size="small" type="info" @click="openTriggerDialog(row)">触发</el-button>
                <el-button size="small" type="danger" @click="confirmRemove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="jobPagination.pageNum"
            v-model:page-size="jobPagination.pageSize"
            :total="jobPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 20px; justify-content: flex-end"
            @size-change="handleJobSizeChange"
            @current-change="handleJobPageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="日志查询" name="logs">
          <el-form :inline="true" class="log-search" @submit.prevent>
            <el-form-item label="任务">
              <el-select
                v-model="logFilters.jobId"
                placeholder="请选择任务"
                style="width: 260px"
                filterable
                clearable
                :loading="jobSelectLoading"
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
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="reloadLogs">
                <el-icon><Search /></el-icon>
                查询日志
              </el-button>
              <el-button @click="resetLogFilters">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="logLoading" :data="logTable" border stripe>
            <el-table-column prop="id" label="日志ID" width="120" />
            <el-table-column prop="jobId" label="任务ID" width="120" />
            <el-table-column prop="executorAddress" label="执行地址" min-width="160" show-overflow-tooltip />
            <el-table-column prop="executorHandler" label="Handler" min-width="160" show-overflow-tooltip />
            <el-table-column label="触发时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.triggerTime) }}
              </template>
            </el-table-column>
            <el-table-column label="执行结果" width="120">
              <template #default="{ row }">
                <el-tag :type="resolveLogTag(row)">{{ resolveLogStatus(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openLogDetail(row)">日志</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="logPagination.pageNum"
            v-model:page-size="logPagination.pageSize"
            :total="logPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 20px; justify-content: flex-end"
            @size-change="handleLogSizeChange"
            @current-change="handleLogPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="jobDialogVisible" :title="jobDialogTitle" width="720px">
      <el-form ref="jobFormRef" :model="jobForm" :rules="jobRules" label-width="120px">
        <el-form-item label="任务描述" prop="jobDesc">
          <el-input v-model="jobForm.jobDesc" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="创建人" prop="author">
          <el-input v-model="jobForm.author" placeholder="请输入创建人" />
        </el-form-item>
        <el-form-item label="报警邮箱">
          <el-input v-model="jobForm.alarmEmail" placeholder="可选" />
        </el-form-item>
        <el-form-item label="调度类型">
          <el-select v-model="jobForm.scheduleType" style="width: 220px">
            <el-option label="CRON" value="CRON" />
          </el-select>
        </el-form-item>
        <el-form-item label="CRON" prop="scheduleConf">
          <el-input v-model="jobForm.scheduleConf" placeholder="请输入 CRON 表达式" />
        </el-form-item>
        <el-form-item label="过期策略">
          <el-select v-model="jobForm.misfireStrategy" style="width: 220px">
            <el-option label="忽略" value="DO_NOTHING" />
            <el-option label="立即执行一次" value="FIRE_ONCE_NOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由策略">
          <el-select v-model="jobForm.executorRouteStrategy" style="width: 220px">
            <el-option label="FIRST" value="FIRST" />
            <el-option label="ROUND" value="ROUND" />
            <el-option label="RANDOM" value="RANDOM" />
            <el-option label="CONSISTENT_HASH" value="CONSISTENT_HASH" />
            <el-option label="LEAST_FREQUENTLY_USED" value="LEAST_FREQUENTLY_USED" />
            <el-option label="LEAST_RECENTLY_USED" value="LEAST_RECENTLY_USED" />
            <el-option label="FAILOVER" value="FAILOVER" />
            <el-option label="BUSYOVER" value="BUSYOVER" />
            <el-option label="SHARDING_BROADCAST" value="SHARDING_BROADCAST" />
          </el-select>
        </el-form-item>
        <el-form-item label="Handler" prop="executorHandler">
          <el-input v-model="jobForm.executorHandler" placeholder="请输入执行器 Handler" />
        </el-form-item>
        <el-form-item label="执行参数">
          <el-input v-model="jobForm.executorParam" placeholder="可选" />
        </el-form-item>
        <el-form-item label="阻塞策略">
          <el-select v-model="jobForm.executorBlockStrategy" style="width: 220px">
            <el-option label="SERIAL_EXECUTION" value="SERIAL_EXECUTION" />
            <el-option label="DISCARD_LATER" value="DISCARD_LATER" />
            <el-option label="COVER_EARLY" value="COVER_EARLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number v-model="jobForm.executorTimeout" :min="0" :max="3600" />
        </el-form-item>
        <el-form-item label="失败重试">
          <el-input-number v-model="jobForm.executorFailRetryCount" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="Glue 类型">
          <el-select v-model="jobForm.glueType" style="width: 220px">
            <el-option label="BEAN" value="BEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="子任务ID">
          <el-input v-model="jobForm.childJobId" placeholder="可选，多个用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jobDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="jobSaving" @click="submitJobForm">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="任务详情" width="720px">
      <el-descriptions v-if="jobDetail" :column="2" border>
        <el-descriptions-item label="任务ID">{{ jobDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="执行器">{{ jobDetail.jobGroup ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ jobDetail.jobDesc }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ jobDetail.author }}</el-descriptions-item>
        <el-descriptions-item label="CRON">{{ jobDetail.scheduleConf }}</el-descriptions-item>
        <el-descriptions-item label="路由策略">{{ jobDetail.executorRouteStrategy }}</el-descriptions-item>
        <el-descriptions-item label="阻塞策略">{{ jobDetail.executorBlockStrategy }}</el-descriptions-item>
        <el-descriptions-item label="超时">{{ jobDetail.executorTimeout ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败重试">{{ jobDetail.executorFailRetryCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="触发状态">
          {{ jobDetail.triggerStatus === 1 ? '运行中' : '已停止' }}
        </el-descriptions-item>
        <el-descriptions-item label="子任务">{{ jobDetail.childJobId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="triggerDialogVisible" title="手动触发" width="520px">
      <el-form :model="triggerForm" label-width="100px">
        <el-form-item label="执行参数">
          <el-input v-model="triggerForm.executorParam" placeholder="可选" />
        </el-form-item>
        <el-form-item label="指定机器">
          <el-input v-model="triggerForm.addressList" placeholder="可选，逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="triggerLoading" @click="submitTrigger">触发</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawerVisible" title="日志详情" size="50%">
      <div class="log-detail">
        <div class="log-toolbar">
          <el-button size="small" @click="reloadLogDetail">刷新</el-button>
          <el-button size="small" type="primary" :disabled="logDetailEnd" @click="loadMoreLog">
            加载更多
          </el-button>
        </div>
        <el-input
          type="textarea"
          :rows="20"
          v-model="logDetailContent"
          readonly
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
    jobTable.value = res.data.data.records
    jobPagination.total = res.data.data.total
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
    const detail = res.data.data
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
    jobDetail.value = res.data.data
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
    const message = res.data.data ? `触发成功：${res.data.data}` : '触发成功'
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
    logTable.value = res.data.data.records
    logPagination.total = res.data.data.total
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
    const detail = res.data.data
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

const resolveLogStatus = (row: XxlJobLog) => {
  if (row.handleCode === 200) return '成功'
  if (row.handleCode === 0 && row.triggerCode === 200) return '执行中'
  if (row.triggerCode && row.triggerCode !== 200) return '触发失败'
  return '失败'
}

const resolveLogTag = (row: XxlJobLog) => {
  if (row.handleCode === 200) return 'success'
  if (row.handleCode === 0 && row.triggerCode === 200) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadAllJobs()
  reloadJobs()
})

const loadAllJobs = async (forceRefresh = false) => {
  jobSelectLoading.value = true
  try {
    const res = await getXxlJobOptions(forceRefresh)
    jobOptions.value = res.data.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载任务列表失败')
  } finally {
    jobSelectLoading.value = false
  }
}
</script>

<style scoped>
.xxl-admin {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.xxl-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.page-header p {
  margin: 4px 0 0;
  color: #8c8c8c;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.log-search {
  margin-bottom: 16px;
}

.log-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-toolbar {
  display: flex;
  gap: 8px;
}
</style>
