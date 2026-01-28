<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- 调用次数统计卡片 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>调用次数统计</span>
          </template>
          <div v-loading="loading.metrics" class="stat-card">
            <div class="stat-item">
              <div class="stat-label">总调用次数</div>
              <div class="stat-value">{{ metrics.totalCalls }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">成功次数</div>
              <div class="stat-value success">{{ metrics.successCalls }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">失败次数</div>
              <div class="stat-value danger">{{ metrics.failedCalls }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 成功率图表 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>模型成功率</span>
          </template>
          <div ref="successRateChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 响应时间图表 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>平均响应时间</span>
          </template>
          <div ref="responseTimeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 模型使用分布图表 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>模型使用分布</span>
          </template>
          <div ref="modelUsageChartRef" style="height: 300px"></div>
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
import { getCallMetrics, getSuccessRate, getResponseTime, getModelUsage } from '@/api/metrics'

const loading = reactive({
  metrics: false,
  successRate: false,
  responseTime: false,
  modelUsage: false
})

const metrics = reactive({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0
})

const successRateChartRef = ref<HTMLElement>()
const responseTimeChartRef = ref<HTMLElement>()
const modelUsageChartRef = ref<HTMLElement>()

let successRateChart: ECharts | null = null
let responseTimeChart: ECharts | null = null
let modelUsageChart: ECharts | null = null

// 获取调用次数统计
const fetchCallMetrics = async () => {
  loading.metrics = true
  try {
    const res = await getCallMetrics()
    const data = res.data.data
    metrics.totalCalls = data.totalCalls
    metrics.successCalls = data.successCalls
    metrics.failedCalls = data.failedCalls
  } catch (error: any) {
    ElMessage.error(error.message || '获取调用统计失败')
  } finally {
    loading.metrics = false
  }
}

// 初始化成功率图表
const initSuccessRateChart = async () => {
  if (!successRateChartRef.value) return

  successRateChart = echarts.init(successRateChartRef.value)

  try {
    const res = await getSuccessRate()
    const data = res.data.data

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      xAxis: {
        type: 'category',
        data: data.map(item => item.modelName)
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [
        {
          name: '成功率',
          type: 'bar',
          data: data.map(item => item.successRate),
          itemStyle: {
            color: '#67C23A'
          }
        }
      ]
    }

    successRateChart.setOption(option)
  } catch (error: any) {
    ElMessage.error(error.message || '获取成功率数据失败')
  }
}

// 初始化响应时间图表
const initResponseTimeChart = async () => {
  if (!responseTimeChartRef.value) return

  responseTimeChart = echarts.init(responseTimeChartRef.value)

  try {
    const res = await getResponseTime()
    const data = res.data.data

    const option = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: data.map(item => item.modelName)
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: '{value}ms'
        }
      },
      series: [
        {
          name: '平均响应时间',
          type: 'line',
          data: data.map(item => item.avgResponseTime),
          smooth: true,
          itemStyle: {
            color: '#409EFF'
          }
        }
      ]
    }

    responseTimeChart.setOption(option)
  } catch (error: any) {
    ElMessage.error(error.message || '获取响应时间数据失败')
  }
}

// 初始化模型使用分布图表
const initModelUsageChart = async () => {
  if (!modelUsageChartRef.value) return

  modelUsageChart = echarts.init(modelUsageChartRef.value)

  try {
    const res = await getModelUsage()
    const data = res.data.data

    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      series: [
        {
          name: '模型使用',
          type: 'pie',
          radius: '60%',
          data: data.map(item => ({
            name: item.modelName,
            value: item.callCount
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    }

    modelUsageChart.setOption(option)
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型使用数据失败')
  }
}

// 窗口大小变化时重新渲染图表
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
.dashboard {
  width: 100%;
}

.stat-card {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-value.success {
  color: #67C23A;
}

.stat-value.danger {
  color: #F56C6C;
}
</style>
