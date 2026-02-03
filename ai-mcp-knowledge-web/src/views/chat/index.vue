<template>
  <div class="gemini-layout">
    <!-- 侧边栏 (Gemini Internal) -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-button class="new-chat-btn" @click="createChat">
          <el-icon><Plus /></el-icon>
          <span>新对话</span>
        </el-button>
        <div class="list-section-title">最近</div>
      </div>
      
      <div class="chat-list">
        <div
          v-for="chat in chats"
          :key="chat.id"
          class="chat-list-item"
          :class="{ active: chat.id === activeChatId }"
          @click="selectChat(chat.id)"
        >
          <el-icon class="chat-icon"><ChatDotRound /></el-icon>
          <span class="chat-title">{{ chat.title || '新对话' }}</span>
          
          <div class="item-actions">
            <el-dropdown trigger="click" @command="(cmd: string) => handleChatCommand(cmd, chat)">
              <el-icon class="more-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="rename">重命名</el-dropdown-item>
                  <el-dropdown-item command="delete" style="color: var(--gemini-danger)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
      
      <div class="sidebar-footer">
        <el-button link class="clear-history-btn" @click="clearAllChats">
          <el-icon><Delete /></el-icon> 清理历史
        </el-button>
      </div>
    </aside>

    <!-- 主对话区域 -->
    <main class="chat-main">
      <!-- 顶部功能栏 (悬浮在对话流之上) -->
      <header class="chat-top-bar">
        <div class="top-controls">
          <div class="model-selector-wrapper">
            <el-dropdown trigger="click" :disabled="isModelLocked" @command="handleModelChange">
              <div class="model-selector-trigger" :class="{ disabled: isModelLocked }">
                <span>{{ resolveSelectedModelName() || 'Gemini 2.0 Pro' }}</span>
                <el-icon><CaretBottom /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="model in models"
                    :key="model.modelId"
                    :command="model.modelId"
                    :class="{ active: model.modelId === selectedModelId }"
                  >
                    {{ model.modelName }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <div class="rag-selector">
            <el-popover placement="bottom" title="私有知识库" :width="280" trigger="click">
              <template #reference>
                <el-button link class="rag-trigger-btn">
                  <el-icon><Files /></el-icon>
                  <span>知识库{{ selectedTags.length ? ` (${selectedTags.length})` : '' }}</span>
                </el-button>
              </template>
              <el-checkbox-group v-model="selectedTags" class="rag-list">
                <el-checkbox v-for="tag in ragTags" :key="tag" :label="tag">{{ tag }}</el-checkbox>
              </el-checkbox-group>
            </el-popover>
          </div>
        </div>
        <div class="top-right-actions">
           <!-- Placeholder for potential future right-side actions -->
        </div>
      </header>

      <!-- 消息列表 -->
      <div ref="chatBodyRef" class="chat-body" @click="handleCodeBlockClick">
        <div class="messages-container">
          <!-- 欢迎页 (空状态) -->
          <div v-if="messages.length === 0" class="welcome-screen">
            <div class="logo-area">
              <img src="https://www.gstatic.com/lamda/images/gemini_sparkle_v002_d4735304ff6292a690345.svg" alt="Logo" class="welcome-logo">
            </div>
            <h1 class="welcome-title">你好！我是 {{ resolveSelectedModelName() || 'Gemini' }}</h1>
            <p class="welcome-subtitle">我可以帮你写代码、分析数据、或者回答你的任何问题。</p>
          </div>

          <!-- 消息流 -->
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="msg.role"
          >
            <div class="avatar-col">
              <div
                v-if="msg.role === 'assistant'"
                class="ai-avatar"
              >
                <img
                  src="https://www.gstatic.com/lamda/images/gemini_sparkle_v002_d4735304ff6292a690345.svg"
                  alt="AI"
                >
              </div>
              <div
                v-else
                class="user-avatar"
              >
                <el-icon><User /></el-icon>
              </div>
            </div>
            
            <div class="content-col">
              <div class="message-info">
                <span class="sender-name">{{ resolveMessageSenderName(msg) }}</span>
                <span class="message-time">{{ formatMessageTime(msg.createdAt) }}</span>
              </div>
              <div class="bubble">
                <div
                  class="content markdown-body"
                  v-html="renderMarkdown(msg.content)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部悬浮输入框 (Floating Capsule) -->
      <footer class="chat-footer">
        <div class="floating-input-capsule no-plugins">
           <div class="input-wrapper">
             <el-input
               v-model="input"
               type="textarea"
               :autosize="{ minRows: 1, maxRows: 8 }"
               :placeholder="`问问 ${resolveSelectedModelName() || 'Gemini'}...`"
               class="gemini-input"
               resize="none"
               @keydown.enter="handleSendShortcut"
             />
           </div>

           <div class="capsule-right">
              <div class="model-info-sm">{{ resolveSelectedModelName() || 'Pro' }}</div>
              <el-button 
                class="send-btn" 
                :loading="sending"
                :disabled="!input.trim() && !sending"
                @click="handleSend"
              >
                <el-icon><Top /></el-icon>
              </el-button>
           </div>
        </div>
        <div class="footer-tip">{{ resolveSelectedModelName() || '模型' }} 的回答不一定精确无误，请注意核实重要事实。</div>
      </footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
// @ts-ignore
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  chatStream,
  getAvailableModels,
  createChatSession,
  updateChatSession,
  deleteChatSession,
  listChatSessions,
  appendChatMessage,
  listChatMessages
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
const chatBodyRef = ref<HTMLElement>()

const formatMessageTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').split('.')[0]
}

const isModelLocked = computed(() => {
  if (!activeChatId.value) {
    return false
  }
  return messages.value.length > 0
})

const resolveSelectedModelName = () => {
  const modelId = selectedModelId.value
  if (!modelId) {
    return ''
  }
  const match = models.value.find(item => item.modelId === modelId)
  return match ? match.modelName : String(modelId)
}

const buildModelLockMessage = () => {
  const modelName = resolveSelectedModelName()
  if (modelName) {
    return `该会话已绑定模型【${modelName}】，为保证对话一致性不可切换模型。如需切换，请新建会话。`
  }
  return '该会话已绑定模型，发送第一条消息后不可切换。如需切换，请新建会话。'
}
const resolveMessageSenderName = (msg: ChatMessage) => {
  if (msg.role === 'user') return '我'
  if (msg.modelId) {
    const match = models.value.find(item => item.modelId === msg.modelId)
    if (match) return match.modelName
  }
  return resolveSelectedModelName() || 'Assistant'
}


const loadSessions = async () => {
  try {
    const res = await listChatSessions(1, 50)
    chats.value = res.data.records || []
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
    messages.value = res.data.records || []
    await nextTick()
    scrollToBottom()
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
    const session = res.data
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
    Object.assign(chat, res.data)
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
    models.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取模型列表失败')
  }
}

const fetchTags = async () => {
  try {
    const res = await listRagTags()
    ragTags.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取标签失败')
  }
}

const handleChatCommand = (command: string, chat: ChatSession) => {
  if (command === 'rename') {
    renameChat(chat)
  } else if (command === 'delete') {
    removeChat(chat)
  }
}

const handleModelChange = (modelId: number) => {
  selectedModelId.value = modelId
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
    Object.assign(currentChat, res.data)
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
  const userMessage = userMessageRes.data
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
  await nextTick()
  scrollToBottom()
  sending.value = true

  const payload: AIRequest = {
    content: userContent,
    modelId: selectedModelId.value,
    sessionId: currentChat.id,
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
    let currentEvent = 'message'
    let eventDataBuffer: string[] = [] // 缓冲当前事件的多行数据
    const usageInfo = {
      promptTokens: null as number | null,
      completionTokens: null as number | null,
      totalTokens: null as number | null
    }

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      
      lines.forEach(line => {
        // 去除行尾回车，保留行首可能的缩进（尽管标准 SSE data 应该在开头，但为了保险）
        const cleanLine = line.trimEnd()
        
        // 空行代表一个事件的结束
        if (!cleanLine) {
          if (eventDataBuffer.length > 0) {
            // 将缓冲的多行数据用换行符连接（还原 SSE 协议中的多行文本）
            const fullData = eventDataBuffer.join('\n')
            eventDataBuffer = [] // 清空缓冲

            if (currentEvent === 'usage') {
              try {
                const usage = JSON.parse(fullData)
                usageInfo.promptTokens = usage.promptTokens ?? usageInfo.promptTokens
                usageInfo.completionTokens = usage.completionTokens ?? usageInfo.completionTokens
                usageInfo.totalTokens = usage.totalTokens ?? usageInfo.totalTokens
                assistantMessage.promptTokens = usageInfo.promptTokens || undefined
                assistantMessage.completionTokens = usageInfo.completionTokens || undefined
                assistantMessage.totalTokens = usageInfo.totalTokens || undefined
              } catch {
                // ignore invalid usage payload
              }
            } else {
              // 默认 message 事件，追加内容
              assistantMessage.content += fullData
            }
          }
          currentEvent = 'message' // 重置事件类型
          return
        }

        // 处理 event: 行
        if (cleanLine.startsWith('event:')) {
          currentEvent = cleanLine.replace(/^event:\s?/, '') || 'message'
          return
        }

        // 处理 data: 行
        if (cleanLine.startsWith('data:')) {
          // 移除前缀 "data:" 和其后的一个可选空格
          const dataContent = cleanLine.replace(/^data: ?/, '')
          eventDataBuffer.push(dataContent)
        }
      })
      
      await nextTick()
      scrollToBottom()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    if (assistantMessage.content) {
      try {
        const saved = await appendChatMessage(currentChat.id, {
          role: 'assistant',
          content: assistantMessage.content,
          modelId: selectedModelId.value,
          promptTokens: assistantMessage.promptTokens,
          completionTokens: assistantMessage.completionTokens,
          totalTokens: assistantMessage.totalTokens
        })
        const index = messages.value.findIndex(item => item.id === assistantMessage.id)
        if (index >= 0) {
          messages.value[index] = saved.data
        }
      } catch (error: any) {
        ElMessage.error(error.message || '保存消息失败')
      }
    }
    sending.value = false
    await nextTick()
    scrollToBottom()
  }
}

const handleSendShortcut = (event: KeyboardEvent) => {
  if (event.metaKey || event.ctrlKey) {
    event.preventDefault()
    handleSend()
  }
}

// clearMessages removed as it was unused

onMounted(() => {
  fetchModels()
  fetchTags()
  loadSessions()
})

const scrollToBottom = () => {
  if (!chatBodyRef.value) return
  chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: (code, language) => {
    const trimmedCode = code.trim()
    const lang = language || 'text'
    let highlighted = ''
    if (language && hljs.getLanguage(language)) {
      try {
        highlighted = hljs.highlight(trimmedCode, { language, ignoreIllegals: true }).value
      } catch (__) {}
    }
    if (!highlighted) {
      highlighted = markdown.utils.escapeHtml(trimmedCode)
    }
    
    // Optimized Gemini Style: Minimalist icon-only copy button with refined feedback
    return `<div class="code-block-wrapper"><div class="code-header"><div class="code-lang-wrapper"><span class="code-lang">${lang}</span></div><div class="copy-btn" title="复制代码"><svg class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg><svg class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display:none"><polyline points="20 6 9 17 4 12"></polyline></svg></div></div><pre class="hljs"><code>${highlighted}</code></pre></div>`
  }
})

const handleCodeBlockClick = async (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const copyBtn = target.closest('.copy-btn') as HTMLElement
  if (!copyBtn) {
    return
  }

  const wrapper = copyBtn.closest('.code-block-wrapper')
  if (!wrapper) return

  const codeBlock = wrapper.querySelector('pre code') as HTMLElement
  if (!codeBlock) return

  const codeText = codeBlock.innerText || codeBlock.textContent || ''
  
  try {
    await navigator.clipboard.writeText(codeText)
    
    const copyIcon = copyBtn.querySelector('.copy-icon') as HTMLElement
    const checkIcon = copyBtn.querySelector('.check-icon') as HTMLElement
    
    if (copyIcon && checkIcon) {
      copyIcon.style.display = 'none'
      checkIcon.style.display = 'block'
      copyBtn.classList.add('copied')
      
      setTimeout(() => {
        copyIcon.style.display = 'block'
        checkIcon.style.display = 'none'
        copyBtn.classList.remove('copied')
      }, 2000)
    }
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const renderMarkdown = (content?: string) => {
  if (!content) return ''
  // 将连续的3个或更多换行符压缩为2个，避免段落间距过大
  const normalized = content.replace(/\n{3,}/g, '\n\n')
  return markdown.render(normalized)
}

watch(selectedModelId, (value, oldValue) => {
  const chat = chats.value.find(item => item.id === activeChatId.value)
  if (!chat) {
    return
  }
  if (isModelLocked.value && value !== chat.modelId) {
    if (oldValue !== undefined) {
      ElMessage.warning(buildModelLockMessage())
    }
    selectedModelId.value = chat.modelId
    return
  }
  chat.modelId = value
})

watch(selectedTags, value => {
  const chat = chats.value.find(item => item.id === activeChatId.value)
  if (chat) {
    chat.ragTags = [...value]
  }
})
</script>

<style scoped>
.gemini-layout {
  --sidebar-width: 260px;
  --header-height: 60px;
  
  display: flex;
  height: 100%;
  width: 100%;
  background-color: var(--gemini-bg-primary);
  color: var(--gemini-text-primary);
  overflow: hidden;
}

/* Internal Sidebar (History) */
.sidebar {
  width: var(--sidebar-width);
  background-color: rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.sidebar-header {
  padding: 16px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.new-chat-btn {
  height: 44px;
  border-radius: 22px;
  background-color: #1e1f20;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--gemini-text-primary);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.new-chat-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.2);
}

.new-chat-btn .el-icon {
  font-size: 18px;
  color: var(--gemini-accent);
}

.list-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--gemini-text-secondary);
  padding: 0 12px;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.chat-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
  padding: 0 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 2px;
  color: var(--gemini-text-primary);
}

.chat-list-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.chat-list-item.active {
  background-color: rgba(138, 180, 248, 0.1);
  color: var(--gemini-accent);
}

.chat-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.chat-title {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-list-item:hover .item-actions {
  opacity: 1;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.clear-history-btn {
  width: 100%;
  justify-content: flex-start;
  color: var(--gemini-text-secondary);
  font-size: 13px;
}

/* Main Content Area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: transparent;
  overflow: hidden;
}

.chat-top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px; /* Align with chat-body padding */
  z-index: 10;
  background: linear-gradient(to bottom, var(--gemini-bg-primary) 30%, transparent);
}

.top-controls {
  display: flex;
  align-items: center;
  gap: 16px; /* Space between model and knowledge base */
}

.model-selector-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 15px;
  color: var(--gemini-text-secondary);
}

.model-selector-trigger:not(.disabled):hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--gemini-text-primary);
}

.rag-trigger-btn {
  color: var(--gemini-text-secondary);
  font-size: 14px;
  gap: 8px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 80px 24px 160px !important; /* Increased top padding to 80px to avoid overlap with 60px header */
  scroll-behavior: smooth;
  display: flex;
  flex-direction: column;
}

.messages-container {
  width: 100%;
  max-width: 1200px !important;
  margin: 0 !important;
}

/* Welcome Screen */
.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding-bottom: 60px;
}

.welcome-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
  animation: sparkle-rotate 10s linear infinite;
}

@keyframes sparkle-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.welcome-title {
  font-size: 32px;
  font-weight: 500;
  margin-bottom: 16px;
  background: linear-gradient(90deg, #4285f4, #9b72cb, #d96570);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.welcome-subtitle {
  font-size: 18px;
  color: var(--gemini-text-secondary);
  max-width: 500px;
}

/* Message Rows */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px !important; /* Tighten up the flow */
  width: 100%;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar-col {
  width: 36px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.ai-avatar img {
  width: 32px;
  height: 32px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #3e4451;
  color: var(--gemini-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.content-col {
  flex: 1;
  max-width: calc(100% - 60px);
  display: flex;
  flex-direction: column;
}

.message-row.user .content-col {
  align-items: flex-end;
}

.message-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.message-row.user .message-info {
  flex-direction: row-reverse;
}

.sender-name {
  font-weight: 600;
}

.message-time {
  color: var(--gemini-text-secondary);
  font-size: 12px;
}

.bubble {
  font-size: 15px; /* Slightly smaller for more refined look */
  line-height: 1.5; /* more compact */
  width: fit-content; /* Ensure bubbles don't stretch */
  word-break: break-word;
}

.message-row.user .bubble {
  background-color: #2f3033;
  padding: 4px 12px !important; /* Minimum padding for capsule look */
  border-radius: 14px;
  border-top-right-radius: 2px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  max-width: 80% !important;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  line-height: normal !important; /* Force tightest line height */
  display: block; /* Ensure wrapping around content */
}

.message-row.user .bubble :deep(p) {
  margin: 0 !important; /* Absolute zero margin for user content */
  padding: 0 !important;
  line-height: 1.4 !important;
}

/* Floating Input Capsule */
.chat-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0 24px 20px !important;
  display: flex;
  flex-direction: column;
  align-items: center; /* Center the capsule */
  background: linear-gradient(transparent, var(--gemini-bg-primary) 80%);
  z-index: 20;
}

.floating-input-capsule {
  width: 100%;
  max-width: 800px !important; /* Balanced, smaller width */
  background-color: #1e1f20;
  border-radius: 28px;
  display: flex !important;
  flex-direction: row !important;
  align-items: center;
  justify-content: space-between;
  padding: 2px 12px;
  gap: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.2s;
  margin: 0 auto;
}

.floating-input-capsule.no-plugins {
  padding: 6px 20px 6px 24px;
}

.floating-input-capsule:focus-within {
  border-color: rgba(138, 180, 248, 0.3);
}



.input-wrapper {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.gemini-input {
  width: 100%;
}

.gemini-input :deep(.el-textarea__inner) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 6px 0 !important; /* Tight internal padding */
  color: var(--gemini-text-primary);
  font-size: 15px; /* Matches bubble font size */
  line-height: 1.4;
  resize: none !important;
  min-height: 20px !important;
}

.capsule-right {
  display: flex !important;
  flex-direction: row !important;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.model-info-sm {
  font-size: 10px; /* Even smaller to avoid bloat */
  font-weight: 600;
  color: var(--gemini-text-secondary);
  background-color: rgba(255, 255, 255, 0.04);
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.send-btn {
  width: 28px; /* Slightly smaller to match slim capsule */
  height: 28px;
  border-radius: 14px;
  padding: 0;
  background-color: var(--gemini-accent);
  border: none;
  color: #131314;
  font-size: 16px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.send-btn:disabled {
  background-color: #3c4043;
  color: #131314;
}

.footer-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--gemini-text-secondary);
  width: 100%;
  text-align: center;
  max-width: 800px; /* Aligns with capsule */
}

/* Markdown Content Styling */
.markdown-body {
  font-family: inherit;
  color: var(--gemini-text-primary);
  line-height: 1.6;
}

.markdown-body :deep(p) {
  margin-bottom: 2px !important; /* Extremely tight to prevent double gaps with code blocks */
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 12px 0 4px 0 !important;
  color: var(--gemini-text-primary);
}

.markdown-body :deep(.code-block-wrapper) {
  margin: 4px 0 12px -20px !important; /* Further left to align with edge */
  width: calc(100% + 20px); /* Fill space */
  border-radius: 6px; /* Narrower radius for edge alignment */
  overflow: hidden;
  background-color: #1e1e20; 
  border: 1px solid var(--gemini-border);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.markdown-body :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 16px; /* Slightly tighter vertical padding */
  background-color: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid var(--gemini-border);
  color: #c4c7c5;
  font-size: 12px;
  user-select: none;
}

.markdown-body :deep(.code-lang) {
  font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
  font-weight: 600;
  text-transform: lowercase;
}

.markdown-body :deep(pre) {
  margin: 0;
  padding: 16px 20px;
  background-color: transparent;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  font-family: "Fira Code", "SauceCodePro Nerd Font", Consolas, Monaco, monospace;
  font-size: 14px;
  line-height: 1.7;
  color: #e8eaed;
  background-color: transparent;
  padding: 0;
  border-radius: 0;
}

.markdown-body :deep(.copy-btn) {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #c4c7c5;
  background-color: transparent;
}

.markdown-body :deep(.copy-btn:hover) {
  background-color: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.markdown-body :deep(.copy-btn.copied) {
  color: #8ab4f8;
  background-color: rgba(138, 180, 248, 0.1);
}

.markdown-body :deep(.copy-btn svg) {
  width: 16px;
  height: 16px;
}
</style>
