export interface GatewayInstance {
  id?: number
  gatewayId: string
  gatewayName: string
  gatewayDesc?: string
  gatewayVersion?: string
  gatewayInstructions?: string
  status?: number
  toolCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface GatewayTool {
  id?: number
  gatewayId: string
  toolName: string
  toolDescription?: string
  httpUrl: string
  httpMethod: string
  httpHeaders?: string
  timeout?: number
  retryTimes?: number
  status?: number
  lastCallSummary?: string
  createdAt?: string
  updatedAt?: string
}

export interface ParamMappingNode {
  id?: number
  parentId?: number | null
  fieldName: string
  mcpType: 'string' | 'number' | 'boolean' | 'object' | 'array'
  mcpDesc?: string
  isRequired?: boolean
  httpPath?: string
  httpLocation?: 'body' | 'query' | 'path' | 'header'
  itemType?: string
  itemRefId?: number | null
  sortOrder?: number
  children?: ParamMappingNode[]
}

export interface SaveGatewayToolRequest {
  id?: number
  gatewayId: string
  toolName: string
  toolDescription?: string
  httpUrl: string
  httpMethod: string
  httpHeaders?: string
  timeout?: number
  retryTimes?: number
  status?: number
  requestMappings?: ParamMappingNode[]
  responseMappings?: ParamMappingNode[]
}

export interface ToolDebugResult {
  success: boolean
  content: string
  errorCode?: string
}

export interface ModelToolBindingState {
  modelId: number
  toolIds: number[]
  globalVisible: boolean
}

export interface ToolOption {
  id: number
  gatewayId: string
  toolName: string
  toolDescription?: string
}

export interface ModelOption {
  id: number
  modelName: string
  modelType: string
}
