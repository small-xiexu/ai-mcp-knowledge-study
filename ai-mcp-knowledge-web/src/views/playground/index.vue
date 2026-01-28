<template>
  <div class="playground">
    <el-row :gutter="20">
      <!-- 左侧配置面板 -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <span>配置</span>
          </template>

          <el-form label-width="80px">
            <el-form-item label="模型选择">
              <el-select
                v-model="config.modelName"
                placeholder="请选择模型"
                style="width: 100%"
                @change="handleModelChange"
              >
                <el-option
                  v-for="model in availableModels"
                  :key="model.modelName"
                  :label="model.modelName"
                  :value="model.modelName"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="任务类型">
              <el-input
                v-model="config.taskType"
                placeholder="请输入任务类型（选填）"
              />
            </el-form-item>

            <el-form-item label="策略">
              <el-select
                v-model="config.strategy"
                placeholder="请选择策略"
                style="width: 100%"
              >
                <el-option label="质量优先" value="QUALITY_FIRST" />
                <el-option label="速度优先" value="SPEED_FIRST" />
                <el-option label="成本优先" value="COST_FIRST" />
              </el-select>
            </el-form-item>

            <el-button
              type="primary"
              style="width: 100%"
              @click="handleClear"
            >
              清空对话
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧对话区域 -->
      <el-col :span="18">
        <el-card style="height: calc(100vh - 140px)">
          <template #header>
            <span>AI 对话测试</span>
          </template>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div
              v-for="(message, index) in messages"
              :key="index"
              :class="['message-item', message.role]"
            >
              <div class="message-content">
                <div class="message-role">
                  {{ message.role === 'user' ? '用户' : 'AI' }}
                </div>
                <div class="message-text">{{ message.content }}</div>
                <div v-if="message.meta" class="message-meta">
                  <el-tag size="small">模型: {{ message.meta.modelUsed }}</el-tag>
                  <el-tag size="small" type="info">
                    响应时间: {{ message.meta.responseTime }}ms
                  </el-tag>
                  <el-tag
                    v-if="message.meta.fallback"
                    size="small"
                    type="warning"
                  >
                    降级
                  </el-tag>
                </div>
              </div>
            </div>

            <div v-if="loading" class="message-item assistant">
              <div class="message-content">
                <div class="message-role">AI</div>
                <div class="message-text">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  正在思考...
                </div>
              </div>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="input-area">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              placeholder="请输入消息..."
              @keydown.enter.ctrl="handleSend"
            />
            <el-button
              type="primary"
              :loading="loading"
              style="margin-top: 10px"
              @click="handleSend"
            >
              发送 (Ctrl+Enter)
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chat, getAvailableModels } from '@/api/ai'
import type { ModelInfo } from '@/types/entity'

interface Message {
  role: 'user' | 'assistant'
  content: string
  meta?: {
    modelUsed: string
    responseTime: number
    fallback: boolean
  }
}

const loading = ref(false)
const inputText = ref('')
const messages = ref<Message[]>([])
const messageListRef = ref<HTMLElement>()
const availableModels = ref<ModelInfo[]>([])

const config = reactive({
  modelName: '',
  taskType: '',
  strategy: 'QUALITY_FIRST'
})

// 获取可用模型列表
const fetchAvailableModels = async () => {
  try {
    const res = await getAvailableModels()
    availableModels.value = res.data.data
    if (availableModels.value.length > 0) {
      config.modelName = availableModels.value[0].modelName
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型列表失败')
  }
}

// 模型变化
const handleModelChange = () => {
  // 可以在这里添加模型切换的逻辑
}

// 发送消息
const handleSend = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入消息')
    return
  }

  const userMessage: Message = {
    role: 'user',
    content: inputText.value
  }

  messages.value.push(userMessage)
  const content = inputText.value
  inputText.value = ''

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  loading.value = true
  try {
    const res = await chat({
      content,
      taskType: config.taskType || undefined,
      strategy: config.strategy
    })

    const data = res.data.data

    const assistantMessage: Message = {
      role: 'assistant',
      content: data.content,
      meta: {
        modelUsed: data.modelUsed,
        responseTime: data.responseTime,
        fallback: data.fallback
      }
    }

    messages.value.push(assistantMessage)

    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error.message || 'AI 调用失败')
  } finally {
    loading.value = false
  }
}

// 清空对话
const handleClear = () => {
  messages.value = []
  inputText.value = ''
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  fetchAvailableModels()
})
</script>

<style scoped>
.playground {
  width: 100%;
}

.message-list {
  height: calc(100vh - 340px);
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.message-item {
  margin-bottom: 20px;
  display: flex;
}

.message-item.user {
  justify-content: flex-end;
}

.message-item.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.message-item.user .message-content {
  background-color: #409eff;
  color: #fff;
}

.message-role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.message-item.user .message-role {
  color: rgba(255, 255, 255, 0.8);
}

.message-text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-meta {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.input-area {
  padding: 0 20px;
}
</style>
