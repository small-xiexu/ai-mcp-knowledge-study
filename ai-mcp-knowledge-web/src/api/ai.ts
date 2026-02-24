import request from '@/utils/request'
import type {
  AIRequest,
  ModelInfo,
  ChatSession,
  ChatSessionCreateRequest,
  ChatSessionUpdateRequest,
  ChatMessage,
  ChatMessageCreateRequest
} from '@/types/entity'

/**
 * 流式 AI 调用
 */
export const chatStream = (data: AIRequest) =>
  fetch('/api/ai/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache'
    },
    cache: 'no-store',
    body: JSON.stringify(data)
  })

/**
 * 获取可用模型列表
 */
export const getAvailableModels = () =>
  request.post<ModelInfo[]>('/ai/models')

/**
 * 创建会话
 */
export const createChatSession = (data: ChatSessionCreateRequest) =>
  request.post<ChatSession>('/ai/sessions', data)

/**
 * 更新会话
 */
export const updateChatSession = (id: number, data: ChatSessionUpdateRequest) =>
  request.post<ChatSession>('/ai/sessions/update', { ...data, id })

/**
 * 删除会话
 */
export const deleteChatSession = (id: number) =>
  request.post<void>('/ai/sessions/delete', { id })

/**
 * 查询会话详情
 */
export const getChatSession = (id: number) =>
  request.post<ChatSession>('/ai/sessions/detail', { id })

/**
 * 分页查询会话
 */
export const listChatSessions = (pageNum = 1, pageSize = 20) =>
  request.post('/ai/sessions/list', { pageNum, pageSize })

/**
 * 追加消息
 */
export const appendChatMessage = (sessionId: number, data: ChatMessageCreateRequest) =>
  request.post<ChatMessage>(`/ai/sessions/${sessionId}/messages`, data)

/**
 * 分页查询消息
 */
export const listChatMessages = (sessionId: number, pageNum = 1, pageSize = 50) =>
  request.post('/ai/sessions/messages/list', { sessionId, pageNum, pageSize })

/**
 * 清空会话消息
 */
export const deleteChatMessages = (sessionId: number) =>
  request.post<void>('/ai/sessions/messages/delete', { id: sessionId })
