import request from '@/utils/request'
import type {
  AIRequest,
  AIResponse,
  ModelInfo,
  ChatSession,
  ChatSessionCreateRequest,
  ChatSessionUpdateRequest,
  ChatMessage,
  ChatMessageCreateRequest
} from '@/types/entity'

/**
 * 通用 AI 调用
 */
export const chat = (data: AIRequest) =>
  request.post<AIResponse>('/ai/chat', data)

/**
 * 流式 AI 调用
 */
export const chatStream = (data: AIRequest) =>
  fetch('/api/ai/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })

/**
 * 获取可用模型列表
 */
export const getAvailableModels = () =>
  request.post<ModelInfo[]>('/ai/models')

/**
 * 获取推荐模型
 */
export const getRecommendedModel = (taskType?: string) =>
  request.post<ModelInfo>('/ai/models/recommend', { taskType })

/**
 * 创建会话
 */
export const createChatSession = (data: ChatSessionCreateRequest) =>
  request.post<ChatSession>('/ai/sessions', data)

/**
 * 更新会话
 */
export const updateChatSession = (id: number, data: ChatSessionUpdateRequest) =>
  request.put<ChatSession>(`/ai/sessions/${id}`, data)

/**
 * 删除会话
 */
export const deleteChatSession = (id: number) =>
  request.delete<void>(`/ai/sessions/${id}`)

/**
 * 查询会话详情
 */
export const getChatSession = (id: number) =>
  request.get<ChatSession>(`/ai/sessions/${id}`)

/**
 * 分页查询会话
 */
export const listChatSessions = (pageNum = 1, pageSize = 20) =>
  request.get('/ai/sessions', { params: { pageNum, pageSize } })

/**
 * 追加消息
 */
export const appendChatMessage = (sessionId: number, data: ChatMessageCreateRequest) =>
  request.post<ChatMessage>(`/ai/sessions/${sessionId}/messages`, data)

/**
 * 分页查询消息
 */
export const listChatMessages = (sessionId: number, pageNum = 1, pageSize = 50) =>
  request.get(`/ai/sessions/${sessionId}/messages`, { params: { pageNum, pageSize } })

/**
 * 清空会话消息
 */
export const deleteChatMessages = (sessionId: number) =>
  request.delete<void>(`/ai/sessions/${sessionId}/messages`)
