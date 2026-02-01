<template>
  <div class="chat-page">
    <el-card class="chat-panel">
      <div class="chat-layout">
        <aside class="chat-sidebar">
          <div class="sidebar-header">
            <div class="title">AI 对话</div>
          </div>
          <div class="sidebar-actions">
            <el-button type="primary" @click="createChat">
              新建对话
            </el-button>
            <el-button @click="clearAllChats">清空全部</el-button>
          </div>
          <div class="chat-list">
            <div
              v-for="chat in chats"
              :key="chat.id"
              class="chat-list-item"
              :class="{ active: chat.id === activeChatId }"
              @click="selectChat(chat.id)"
            >
              <div class="chat-title-row">
                <div class="chat-title">{{ chat.title }}</div>
                <div class="chat-actions">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click.stop="renameChat(chat)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click.stop="removeChat(chat)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              <div class="chat-meta">{{ formatChatTime(chat.createdAt) }}</div>
            </div>
            <div v-if="chats.length === 0" class="chat-empty">
              暂无对话
            </div>
          </div>
        </aside>

        <section class="chat-main">
          <div class="chat-header">
            <div class="left">
              <el-select v-model="selectedModelId" placeholder="选择模型" clearable style="width: 220px">
                <el-option
                  v-for="model in models"
                  :key="model.modelId"
                  :label="`${model.modelName} (${model.modelType})`"
                  :value="model.modelId"
                />
              </el-select>
              <el-select
                v-model="selectedTags"
                multiple
                clearable
                collapse-tags
                placeholder="选择知识库标签"
                style="width: 300px; margin-left: 12px"
              >
                <el-option
                  v-for="tag in ragTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </div>
            <div class="right">
              <el-button @click="clearMessages">清空当前</el-button>
            </div>
          </div>

          <div class="chat-body">
            <div v-if="messages.length === 0" class="chat-placeholder">
              输入问题开始对话
            </div>
            <div v-for="msg in messages" :key="msg.id" class="chat-item" :class="msg.role">
              <div class="bubble">
                <div class="role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
                <div class="content">{{ msg.content }}</div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="input"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 6 }"
              placeholder="输入你的问题..."
            />
            <div class="actions">
              <el-button type="primary" :loading="sending" @click="handleSend">
                发送
              </el-button>
            </div>
          </div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  chatStream,
  getAvailableModels,
  createChatSession,
  updateChatSession,
  deleteChatSession,
  listChatSessions,
  appendChatMessage,
  listChatMessages,
  deleteChatMessages
} from '@/api/ai'
import { listRagTags } from '@/api/rag'
import type { AIRequest, ModelInfo, ChatSession, ChatMessage } from '@/types/entity'

const models = ref<ModelInfo[]>([])
const ragTags = ref<string[]>([])
const selectedModelId = ref<number | undefined>()
const selectedTags = ref<string[]>([])
const chats = ref<ChatSession[]>([])
const activeChatId = ref<number | null>(null)
const input = ref('')
const sending = ref(false)
const messages = ref<ChatMessage[]>([])

const formatChatTime = (value: string) => {
  if (!value) return '-'
  return value.replace('T', ' ')
}

const loadSessions = async () => {
  try {
    const res = await listChatSessions(1, 50)
    chats.value = res.data.data.records || []
    if (chats.value.length > 0 && activeChatId.value === null) {
      await selectChat(chats.value[0].id)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取会话失败')
  }
}

const loadMessages = async (sessionId: number) => {
  try {
    const res = await listChatMessages(sessionId, 1, 200)
    messages.value = res.data.data.records || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取消息失败')
  }
}

const createChat = async () => {
  try {
    const res = await createChatSession({
      title: '新对话',
      modelId: selectedModelId.value,
      ragTags: selectedTags.value
    })
    const session = res.data.data
    chats.value.unshift(session)
    await selectChat(session.id)
  } catch (error: any) {
    ElMessage.error(error.message || '创建会话失败')
  }
}

const selectChat = async (id: number) => {
  activeChatId.value = id
  const chat = chats.value.find(item => item.id === id)
  if (chat) {
    selectedModelId.value = chat.modelId
    selectedTags.value = chat.ragTags || []
  }
  await loadMessages(id)
}

const clearAllChats = async () => {
  const toDelete = [...chats.value]
  for (const chat of toDelete) {
    await deleteChatSession(chat.id)
  }
  chats.value = []
  messages.value = []
  activeChatId.value = null
  await createChat()
}

const renameChat = async (chat: ChatSession) => {
  try {
    const result = await ElMessageBox.prompt('请输入新标题', '编辑会话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: chat.title || '新对话',
      inputValidator: (value: string) => value.trim().length > 0 || '标题不能为空'
    })
    const title = result.value.trim()
    const res = await updateChatSession(chat.id, {
      title,
      modelId: chat.modelId,
      ragTags: chat.ragTags
    })
    Object.assign(chat, res.data.data)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '更新标题失败')
    }
  }
}

const removeChat = async (chat: ChatSession) => {
  try {
    await ElMessageBox.confirm(`确定删除会话 "${chat.title}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatSession(chat.id)
    chats.value = chats.value.filter(item => item.id !== chat.id)
    if (activeChatId.value === chat.id) {
      messages.value = []
      activeChatId.value = chats.value.length > 0 ? chats.value[0].id : null
      if (activeChatId.value) {
        await selectChat(activeChatId.value)
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const fetchModels = async () => {
  try {
    const res = await getAvailableModels()
    models.value = res.data.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型列表失败')
  }
}

const fetchTags = async () => {
  try {
    const res = await listRagTags()
    ragTags.value = res.data.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取标签失败')
  }
}

const handleSend = async () => {
  if (!input.value.trim()) {
    return
  }
  if (sending.value) {
    return
  }
  if (!activeChatId.value) {
    await createChat()
  }
  const userContent = input.value.trim()
  input.value = ''
  const currentChat = chats.value.find(item => item.id === activeChatId.value)
  if (!currentChat) {
    return
  }
  if (!currentChat.title || currentChat.title === '新对话') {
    const newTitle = userContent.slice(0, 20)
    const res = await updateChatSession(currentChat.id, {
      title: newTitle,
      modelId: selectedModelId.value,
      ragTags: selectedTags.value
    })
    Object.assign(currentChat, res.data.data)
  } else {
    await updateChatSession(currentChat.id, {
      title: currentChat.title,
      modelId: selectedModelId.value,
      ragTags: selectedTags.value
    })
  }

  const userMessageRes = await appendChatMessage(currentChat.id, {
    role: 'user',
    content: userContent,
    modelId: selectedModelId.value
  })
  const userMessage = userMessageRes.data.data
  messages.value.push(userMessage)
  const assistantMessage: ChatMessage = {
    id: Date.now(),
    sessionId: currentChat.id,
    role: 'assistant',
    content: '',
    modelId: selectedModelId.value,
    createdAt: new Date().toISOString()
  }
  messages.value.push(assistantMessage)
  sending.value = true

  const payload: AIRequest = {
    content: userContent,
    modelId: selectedModelId.value,
    ragTags: selectedTags.value
  }

  try {
    const response = await chatStream(payload)
    if (!response.body) {
      throw new Error('响应为空')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      lines.forEach(line => {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) return
        const data = trimmed.replace(/^data:\s?/, '')
        if (!data) return
        assistantMessage.content += data
      })
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    if (assistantMessage.content) {
      try {
        const saved = await appendChatMessage(currentChat.id, {
          role: 'assistant',
          content: assistantMessage.content,
          modelId: selectedModelId.value
        })
        const index = messages.value.findIndex(item => item.id === assistantMessage.id)
        if (index >= 0) {
          messages.value[index] = saved.data.data
        }
      } catch (error: any) {
        ElMessage.error(error.message || '保存消息失败')
      }
    }
    sending.value = false
  }
}

const clearMessages = async () => {
  if (!activeChatId.value) {
    return
  }
  try {
    await deleteChatMessages(activeChatId.value)
    messages.value = []
  } catch (error: any) {
    ElMessage.error(error.message || '清空失败')
  }
}

onMounted(() => {
  fetchModels()
  fetchTags()
  loadSessions()
})

watch(selectedModelId, value => {
  const chat = chats.value.find(item => item.id === activeChatId.value)
  if (chat) {
    chat.modelId = value
  }
})

watch(selectedTags, value => {
  const chat = chats.value.find(item => item.id === activeChatId.value)
  if (chat) {
    chat.ragTags = [...value]
  }
})
</script>

<style scoped>
.chat-page {
  width: 100%;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.chat-layout {
  display: flex;
  height: 100%;
  gap: 16px;
}

.chat-sidebar {
  width: 240px;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sidebar-header .title {
  font-weight: 600;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.chat-list-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid transparent;
}

.chat-list-item.active {
  background: #f0f7ff;
  border-color: #c6e2ff;
}

.chat-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.chat-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.chat-empty {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 12px 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.chat-item {
  display: flex;
  margin-bottom: 12px;
}

.chat-item.user {
  justify-content: flex-end;
}

.chat-item.assistant {
  justify-content: flex-start;
}

.chat-item .bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #ebeef5;
}

.chat-item.user .bubble {
  background: #ecf5ff;
  border-color: #d9ecff;
}

.chat-item.assistant .bubble {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.chat-item .role {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #606266;
}

.chat-item.user .role {
  color: #409eff;
}

.chat-item.assistant .role {
  color: #67c23a;
}

.chat-item .content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.chat-placeholder {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.chat-input {
  margin-top: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
