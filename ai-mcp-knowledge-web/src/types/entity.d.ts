/**
 * 模型配置
 */
export interface ModelConfig {
  id: number
  modelName: string
  modelType: string
  apiKey?: string
  baseUrl: string
  enabled: boolean
  priority: number
  capability?: ModelCapability
  createdAt: string
  updatedAt: string
}

/**
 * 模型能力
 */
export interface ModelCapability {
  maxInputTokens: number
  maxOutputTokens: number
  supportFunctionCalling: boolean
  supportVision: boolean
  supportStreaming: boolean
  qualityScore: number
}

/**
 * 调用日志
 */
export interface CallLog {
  id: number
  traceId: string
  modelId: number
  taskType: string
  requestTime: string
  responseTime: string
  status: string
  promptTokens: number
  completionTokens: number
  errorMessage?: string
}

/**
 * 模型信息
 */
export interface ModelInfo {
  modelName: string
  modelType: string
  enabled: boolean
  priority: number
}

/**
 * AI 请求
 */
export interface AIRequest {
  content: string
  taskType?: string
  strategy?: string
}

/**
 * AI 响应
 */
export interface AIResponse {
  success: boolean
  content: string
  modelUsed: string
  responseTime: number
  fallback: boolean
  errorMessage?: string
}
