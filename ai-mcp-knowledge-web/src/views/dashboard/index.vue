<template>
  <div class="gemini-container dashboard-modern">
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">监控看板</h2>
        <div class="header-subtitle">实时洞悉您的 AI 能力调度与模型运行状况</div>
      </div>
      <div class="header-actions">
         <el-tag effect="light" class="status-badge" type="success">系统运行良好</el-tag>
      </div>
    </div>

    <!-- 顶层指标卡片 (四宫格) -->
    <el-row :gutter="20" class="stats-grid">
      <el-col :span="6">
        <div class="cool-stat-card total">
          <div class="card-icon"><el-icon><DataLine /></el-icon></div>
          <div class="card-info">
            <div class="stat-label">总调用量</div>
            <div class="stat-value">{{ metrics.totalCalls }}</div>
          </div>
          <div class="card-glow"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="cool-stat-card success">
          <div class="card-icon"><el-icon><CircleCheckFilled /></el-icon></div>
          <div class="card-info">
            <div class="stat-label">成功调用</div>
            <div class="stat-value">{{ metrics.successCalls }}</div>
          </div>
          <div class="card-glow"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="cool-stat-card failed">
          <div class="card-icon"><el-icon><CircleCloseFilled /></el-icon></div>
          <div class="card-info">
            <div class="stat-label">异常次数</div>
            <div class="stat-value">{{ metrics.failedCalls }}</div>
          </div>
          <div class="card-glow"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="cool-stat-card fallback">
          <div class="card-icon"><el-icon><Refresh /></el-icon></div>
          <div class="card-info">
            <div class="stat-label">服务降级</div>
            <div class="stat-value">{{ metrics.fallbackCalls }}</div>
          </div>
          <div class="card-glow"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="24" class="charts-row">
      <el-col :span="12">
        <el-card class="gemini-card chart-card" shadow="never">
          <template #header>
            <div class="card-header-main">
              <el-icon><Histogram /></el-icon>
              <span>模型负载与成功率</span>
            </div>
          </template>
          <div v-loading="loading.successRate" class="chart-container">
            <div ref="successRateChartRef" style="height: 320px" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="gemini-card chart-card" shadow="never">
          <template #header>
            <div class="card-header-main">
              <el-icon><PieChart /></el-icon>
              <span>模型使用权重分布</span>
            </div>
          </template>
          <div v-loading="loading.modelUsage" class="chart-container">
            <div ref="modelUsageChartRef" style="height: 320px" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="charts-row" style="margin-top: 24px">
      <el-col :span="24">
        <el-card class="gemini-card chart-card full-width" shadow="never">
          <template #header>
            <div class="card-header-main">
              <el-icon><TrendCharts /></el-icon>
              <span>响应耗时性能趋势</span>
            </div>
          </template>
          <div v-loading="loading.responseTime" class="chart-container">
            <div ref="responseTimeChartRef" style="height: 350px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  DataLine,
  CircleCheckFilled,
  CircleCloseFilled,
  Histogram,
  PieChart,
  TrendCharts,
  Refresh
} from '@element-plus/icons-vue'
import { getCallMetrics, getSuccessRate, getResponseTime, getModelUsage } from '@/api/metrics'
import { getAvailableModels } from '@/api/ai'

const loading = reactive({
  metrics: false,
  successRate: false,
  responseTime: false,
  modelUsage: false
})

const metrics = reactive({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0,
  fallbackCalls: 0
})

const successRateChartRef = ref<HTMLElement>()
const responseTimeChartRef = ref<HTMLElement>()
const modelUsageChartRef = ref<HTMLElement>()

let successRateChart: ECharts | null = null
let responseTimeChart: ECharts | null = null
let modelUsageChart: ECharts | null = null

// Gemini Colors Palette
const geminiColors = {
  primary: '#8ab4f8',
  secondary: '#aecbfa',
  accent: '#c2e7ff',
  success: '#81c995',
  warning: '#fdd663',
  danger: '#f28b82',
  bg: '#1e1f20',
  text: '#e8eaed',
  textSecondary: '#9aa0a6'
}

const formatLocalDateTime = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

const buildDefaultRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 7)
  return {
    startTime: formatLocalDateTime(start),
    endTime: formatLocalDateTime(end)
  }
}

const fetchCallMetrics = async () => {
  loading.metrics = true
  try {
    const res = await getCallMetrics(buildDefaultRange())
    const data = res.data
    metrics.totalCalls = data.totalCalls
    metrics.successCalls = data.successCalls
    metrics.failedCalls = data.failedCalls
    metrics.fallbackCalls = data.fallbackCalls
  } catch (error: any) {
    ElMessage.error(error.message || '获取调用统计失败')
  } finally {
    loading.metrics = false
  }
}

const initSuccessRateChart = async () => {
  if (!successRateChartRef.value) return
  successRateChart = echarts.init(successRateChartRef.value)
  try {
    const res = await getSuccessRate(buildDefaultRange())
    const data = res.data
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'none' },
        backgroundColor: 'rgba(30, 31, 32, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: geminiColors.text },
        valueFormatter: (value: number) => value.toFixed(2) + '%'
      },
      grid: {
        top: '15%',
        left: '5%',
        right: '5%',
        bottom: '5%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['系统成功率'],
        axisLabel: { color: geminiColors.textSecondary, fontSize: 13 },
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: { color: geminiColors.textSecondary },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)', type: 'dashed' } }
      },
      series: [
        {
          name: '成功率',
          type: 'bar',
          data: [data?.successRate ?? 0],
          barWidth: '35%',
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#81c995' },
              { offset: 1, color: '#34a853' }
            ]),
            shadowBlur: 10,
            shadowColor: 'rgba(129, 201, 149, 0.3)'
          },
          label: {
            show: true,
            position: 'top',
            formatter: (params: any) => params.value.toFixed(2) + '%',
            color: geminiColors.text,
            fontWeight: 600
          }
        }
      ]
    }
    successRateChart.setOption(option)
  } catch (error: any) {
    ElMessage.error(error.message || '获取成功率失败')
  }
}

const initResponseTimeChart = async () => {
  if (!responseTimeChartRef.value) return
  responseTimeChart = echarts.init(responseTimeChartRef.value)
  try {
    const res = await getResponseTime(buildDefaultRange())
    const data = res.data
    const option = {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(30, 31, 32, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: geminiColors.text }
      },
      grid: {
        top: '15%',
        left: '5%',
        right: '5%',
        bottom: '5%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['平均', '最大', '最小'],
        boundaryGap: true,
        axisLabel: { color: geminiColors.textSecondary },
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: geminiColors.textSecondary, formatter: '{value}ms' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
      },
      series: [
        {
          name: '耗时',
          type: 'line',
          smooth: true,
          showSymbol: true,
          symbolSize: 8,
          data: [
            data?.avgResponseTime ?? 0,
            data?.maxResponseTime ?? 0,
            data?.minResponseTime ?? 0
          ],
          lineStyle: {
            width: 4,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#8ab4f8' },
              { offset: 1, color: '#c2e7ff' }
            ]),
            shadowBlur: 10,
            shadowColor: 'rgba(138, 180, 248, 0.4)'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(138, 180, 248, 0.2)' },
              { offset: 1, color: 'transparent' }
            ])
          },
          itemStyle: { color: '#8ab4f8', borderWidth: 2, borderColor: '#fff' }
        }
      ]
    }
    responseTimeChart.setOption(option)
  } catch (error: any) {
    ElMessage.error(error.message || '获取耗时数据失败')
  }
}

const initModelUsageChart = async () => {
  if (!modelUsageChartRef.value) return
  modelUsageChart = echarts.init(modelUsageChartRef.value)
  try {
    const res = await getModelUsage(buildDefaultRange())
    const data = res.data
    const modelRes = await getAvailableModels()
    const modelList = modelRes.data || []
    const modelNameMap = new Map(modelList.map((item: any) => [item.modelId, item.modelName]))
    const totalCalls = data.reduce((sum: number, item: any) => sum + item.callCount, 0)

    const option = {
      backgroundColor: 'transparent',
      color: ['#8ab4f8', '#81c995', '#fdd663', '#f28b82', '#c2e7ff', '#ff8bcb'],
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c}次 ({d}%)',
        backgroundColor: 'rgba(30, 31, 32, 0.95)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        textStyle: { color: '#fff' }
      },
      legend: {
        orient: 'vertical',
        right: '5%',
        top: 'middle',
        itemGap: 15,
        textStyle: { color: geminiColors.textSecondary, fontSize: 13 }
      },
      series: [
        {
          name: '权重分布',
          type: 'pie',
          radius: ['50%', '75%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#1e1f20',
            borderWidth: 2
          },
          label: {
            show: true,
            position: 'center',
            formatter: `{total|${totalCalls}}\n{label|活跃调用}`,
            rich: {
              total: {
                fontSize: 28,
                fontWeight: 700,
                color: '#fff',
                lineHeight: 32
              },
              label: {
                fontSize: 12,
                color: geminiColors.textSecondary,
                lineHeight: 18
              }
            }
          },
          emphasis: {
            label: {
              show: true,
              formatter: `{total|${totalCalls}}\n{label|活跃调用}`,
              rich: {
                total: {
                  fontSize: 28,
                  fontWeight: 700,
                  color: '#fff',
                  lineHeight: 32
                },
                label: {
                  fontSize: 12,
                  color: geminiColors.textSecondary,
                  lineHeight: 18
                }
              }
            }
          },
          data: data.map(item => ({
            name: modelNameMap.get(item.modelId) || `Model-${item.modelId}`,
            value: item.callCount
          }))
        }
      ]
    }
    modelUsageChart.setOption(option)
  } catch (error) {
    ElMessage.error('模型数据解析失败')
  }
}

const handleResize = () => {
  successRateChart?.resize()
  responseTimeChart?.resize()
  modelUsageChart?.resize()
}

onMounted(() => {
  fetchCallMetrics()
  initSuccessRateChart()
  initResponseTimeChart()
  initModelUsageChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  successRateChart?.dispose()
  responseTimeChart?.dispose()
  modelUsageChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-modern {
  padding: 24px;
  background-color: var(--gemini-bg-primary);
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
}

.header-subtitle {
  color: var(--gemini-text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.status-badge {
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  background-color: rgba(129, 201, 149, 0.1);
  border: 1px solid rgba(129, 201, 149, 0.2);
}

/* Cool Metrics Cards */
.stats-grid {
  margin-bottom: 32px;
}

.cool-stat-card {
  position: relative;
  height: 100px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
}

.cool-stat-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.15);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
  z-index: 1;
}

/* Card Specific Gradients & Glows */
.total .card-icon { background: rgba(138, 180, 248, 0.1); color: #8ab4f8; }
.success .card-icon { background: rgba(129, 201, 149, 0.1); color: #81c995; }
.failed .card-icon { background: rgba(242, 139, 130, 0.1); color: #f28b82; }
.fallback .card-icon { background: rgba(253, 214, 99, 0.1); color: #fdd663; }

.stat-label {
  font-size: 13px;
  color: var(--gemini-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--gemini-text-primary);
  line-height: 1;
}

.card-glow {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: radial-gradient(circle at 100% 0%, rgba(255,255,255,0.05) 0%, transparent 50%);
  pointer-events: none;
}

/* Common Chart Card Style */
.chart-card {
  height: 420px;
  background: rgba(40, 42, 45, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  overflow: hidden;
}

.chart-card :deep(.el-card__body) {
  padding: 0 !important;
}

.card-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  color: var(--gemini-text-primary);
}

.card-header-main .el-icon {
  font-size: 20px;
  color: var(--gemini-accent);
}

.chart-container {
  padding: 20px;
}

.full-width {
  height: 450px;
}
</style>

