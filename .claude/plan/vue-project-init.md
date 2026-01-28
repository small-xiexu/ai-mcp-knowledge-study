# AI MCP Knowledge Web - Vue 3 前端项目实施计划

> **生成时间**：2026-01-27
> **方案**：方案 A（运维中心型管理后台）
> **技术栈**：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Axios

---

## 1. 项目初始化步骤

### 1.1 创建项目

```bash
# 1. 创建 Vite + Vue 3 + TypeScript 项目
npm create vite@latest ai-mcp-knowledge-web -- --template vue-ts

# 2. 进入目录
cd ai-mcp-knowledge-web

# 3. 安装核心依赖
npm install element-plus @element-plus/icons-vue axios pinia vue-router echarts

# 4. 安装开发依赖
npm install -D sass sass-loader eslint prettier typescript
```

### 1.2 关键配置

**Vite Config (`vite.config.ts`)**：
- 配置 `@` 别名指向 `src`
- 配置开发服务器代理（Proxy）：`/api` → `http://localhost:8080/api`

**TypeScript (`tsconfig.json`)**：
- 启用严格模式 (`strict: true`)
- 配置路径映射

---

## 2. 目录结构设计

```
ai-mcp-knowledge-web/
├── public/
├── src/
│   ├── api/                  # API 接口定义（与后端 Controller 对应）
│   │   ├── ai.ts             # AICallController
│   │   ├── audit.ts          # AuditController
│   │   ├── metrics.ts        # MetricsController
│   │   ├── model.ts          # ModelConfigController
│   │   └── task.ts           # TaskTypeController
│   ├── assets/               # 静态资源（Logo, global css）
│   ├── components/           # 全局公共组件
│   │   ├── Layout/           # 布局组件（Sidebar, Navbar）
│   │   │   ├── MainLayout.vue
│   │   │   ├── Sidebar.vue
│   │   │   └── Navbar.vue
│   │   ├── JsonViewer/       # JSON 展示组件（用于审计日志）
│   │   │   └── JsonViewer.vue
│   │   └── StatusTag/        # 状态标签组件
│   │       └── StatusTag.vue
│   ├── hooks/                # 组合式函数
│   │   ├── useChart.ts       # ECharts 封装
│   │   └── useTable.ts       # 表格分页封装
│   ├── router/               # 路由配置
│   │   ├── index.ts          # 路由实例
│   │   └── routes.ts         # 路由表
│   ├── store/                # Pinia 状态管理
│   │   ├── app.ts            # 应用状态
│   │   └── model.ts          # 模型状态
│   ├── types/                # TypeScript 类型定义
│   │   ├── api.d.ts          # 通用响应结构（Result, PageResult）
│   │   └── entity.d.ts       # 实体定义（ModelConfig, CallLog...）
│   ├── utils/                # 工具库
│   │   ├── request.ts        # Axios 封装（拦截器）
│   │   ├── format.ts         # 日期/数据格式化
│   │   └── constants.ts      # 常量定义
│   ├── views/                # 页面视图（Smart Components）
│   │   ├── dashboard/        # 仪表盘
│   │   │   └── index.vue
│   │   ├── model/            # 模型管理
│   │   │   ├── index.vue     # 模型列表
│   │   │   └── components/   # 模型相关组件
│   │   │       ├── ModelForm.vue
│   │   │       └── ModelTable.vue
│   │   ├── task/             # 任务策略配置
│   │   │   └── index.vue
│   │   ├── audit/            # 审计日志
│   │   │   └── index.vue
│   │   └── playground/       # AI 调试演练场
│   │       └── index.vue
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── .eslintrc.js              # ESLint 配置
├── .prettierrc.js            # Prettier 配置
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 3. 核心文件清单

| 文件路径 | 作用 | 核心内容概要 |
|---------|------|-------------|
| `src/utils/request.ts` | Axios 实例 | 配置 baseURL，Request 拦截器（Token 注入），Response 拦截器（解包 Java `Result`，统一处理错误） |
| `src/types/api.d.ts` | 泛型接口 | 定义 `Result<T>` 和 `PageResult<T>` |
| `src/components/Layout/MainLayout.vue` | 布局框架 | 包含侧边栏（`ElMenu`）、顶部导航（`ElHeader`）和内容区（`RouterView`） |
| `src/router/routes.ts` | 路由表 | 定义静态路由和菜单元数据（`meta: { title, icon }`） |
| `src/store/app.ts` | 应用状态 | 管理侧边栏折叠状态、当前主题、全局 Loading 状态 |

---

## 4. 路由规划

| 路径 | 组件 | 页面名称 | 说明 |
|------|------|---------|------|
| `/` | - | Root | 重定向至 `/dashboard` |
| `/dashboard` | `views/dashboard/index.vue` | 监控看板 | 展示 Metrics 统计图表，系统健康状态 |
| `/models` | `views/model/index.vue` | 模型管理 | 模型列表（分页），提供新增/编辑/启用/禁用入口 |
| `/tasks` | `views/task/index.vue` | 任务策略 | 任务类型定义，绑定模型策略 |
| `/audit` | `views/audit/index.vue` | 审计日志 | 调用日志查询（分页、筛选），查看详细 JSON |
| `/playground` | `views/playground/index.vue` | 调试演练 | 提供一个 Chat 界面，用于测试不同模型/任务的响应 |

---

## 5. API 接口封装

### 5.1 通用类型（`src/types/api.d.ts`）

```typescript
export interface Result<T = any> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}
```

### 5.2 实体类型（`src/types/entity.d.ts`）

```typescript
export interface ModelConfig {
  id: number;
  modelName: string;
  modelType: string;
  baseUrl: string;
  enabled: boolean;
  priority: number;
  capability?: ModelCapability;
  createdAt: string;
  updatedAt: string;
}

export interface ModelCapability {
  maxInputTokens: number;
  maxOutputTokens: number;
  supportFunctionCalling: boolean;
  supportVision: boolean;
  supportStreaming: boolean;
  qualityScore: number;
}

export interface CallLog {
  id: number;
  traceId: string;
  modelId: number;
  taskType: string;
  requestTime: string;
  responseTime: string;
  status: string;
  promptTokens: number;
  completionTokens: number;
  errorMessage?: string;
}
```

### 5.3 Service 层示例（`src/api/model.ts`）

```typescript
import request from '@/utils/request';
import type { ModelConfig } from '@/types/entity';
import type { Result, PageResult } from '@/types/api';

// 查询模型列表
export const getModelList = (params: any) =>
  request.get<Result<PageResult<ModelConfig>>>('/models', { params });

// 查询单个模型
export const getModelById = (id: number) =>
  request.get<Result<ModelConfig>>(`/models/${id}`);

// 创建模型
export const createModel = (data: Partial<ModelConfig>) =>
  request.post<Result<ModelConfig>>('/models', data);

// 更新模型
export const updateModel = (id: number, data: Partial<ModelConfig>) =>
  request.put<Result<ModelConfig>>(`/models/${id}`, data);

// 删除模型
export const deleteModel = (id: number) =>
  request.delete<Result<void>>(`/models/${id}`);

// 启用/禁用模型
export const toggleModelStatus = (id: number, enable: boolean) =>
  request.put<Result<ModelConfig>>(`/models/${id}/${enable ? 'enable' : 'disable'}`);
```

---

## 6. 组件拆分

### 6.1 公共组件（Common Components）

1. **`StatusTag.vue`**
   - 接收 `status` (string/enum) 和 `type` (success/danger/warning)
   - 统一显示状态样式（如启用/禁用，成功/失败）

2. **`JsonViewer.vue`**
   - 封装 JSON 展示组件
   - 用于在审计日志中查看 `request_params` 和 `response_body`

3. **`ConfigForm.vue`**
   - 动态表单组件
   - 根据选择的 `Provider` (OpenAI/Gemini/Anthropic) 渲染不同的配置字段

### 6.2 业务/页面组件（Smart Components）

1. **`ModelList.vue`**
   - 包含搜索栏、`ElTable`、分页器
   - 处理"启用/禁用"的逻辑

2. **`MetricsChart.vue`**
   - 封装 ECharts
   - 接收数据并在图表大小变化时自动 resize

3. **`ChatBox.vue`**（用于 Playground）
   - 模拟聊天界面
   - 包含消息流展示、输入框、模型选择下拉框

---

## 7. 状态管理（Pinia）

### 7.1 `useAppStore`（`src/store/app.ts`）

```typescript
import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    theme: 'light' as 'light' | 'dark',
    loading: false,
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    },
    setTheme(theme: 'light' | 'dark') {
      this.theme = theme;
    },
    setLoading(loading: boolean) {
      this.loading = loading;
    },
  },
});
```

### 7.2 `useModelStore`（`src/store/model.ts`）

```typescript
import { defineStore } from 'pinia';
import { getAvailableModels } from '@/api/ai';
import type { ModelInfo } from '@/types/entity';

export const useModelStore = defineStore('model', {
  state: () => ({
    availableModels: [] as ModelInfo[],
  }),
  actions: {
    async fetchAvailableModels() {
      const res = await getAvailableModels();
      this.availableModels = res.data.data;
    },
  },
});
```

---

## 8. 实施顺序（4个阶段）

### 阶段一：骨架搭建（Skeleton）

**任务**：
1. 初始化 Vite 项目，安装依赖
2. 配置 Axios 拦截器和 Proxy
3. 搭建 MainLayout 和基础路由
4. 创建空白页面占位

**交付物**：
- 可运行的空壳应用
- 具备导航栏和页面占位
- 路由跳转正常

---

### 阶段二：基础资源管理（CRUD）

**任务**：
1. 定义 `ModelConfig` 相关 TS 类型
2. 实现 `src/api/model.ts`
3. 开发**模型管理页面**：
   - 列表展示（分页）
   - 新增弹窗
   - 编辑功能
   - 状态切换（启用/禁用）
4. 开发**任务类型页面**：简单的 CRUD

**交付物**：
- 可以配置 AI 模型参数和任务类型的后台
- 完整的 CRUD 功能

---

### 阶段三：可观测性（Observability）

**任务**：
1. 实现 `src/api/audit.ts` 和 `src/api/metrics.ts`
2. 开发**审计日志页面**：
   - 重点是 JSON 数据的展示
   - 按 TraceID 搜索
   - 分页查询
3. 开发**监控看板**：
   - 集成 ECharts
   - 展示成功率和响应时间趋势
   - 模型使用分布

**交付物**：
- 具备完整的日志查询和系统健康监控能力
- 可视化图表展示

---

### 阶段四：演练与集成（Playground）

**任务**：
1. 实现 `src/api/ai.ts`
2. 开发**Playground 页面**：
   - 简单的聊天 UI
   - 调用 `/api/ai/chat` 接口
   - 模型选择
   - 任务类型选择
3. 联调测试：
   - 在前端配置模型
   - 在 Playground 测试调用
   - 在审计页查看日志

**交付物**：
- 完整的闭环演示系统
- 端到端功能验证

---

## 9. 技术要点

### 9.1 Axios 拦截器

**Request 拦截器**：
- 添加 Token（如果有）
- 显示 Loading

**Response 拦截器**：
- 解包 Java `Result<T>` 结构
- 统一处理 4xx/5xx 错误
- 使用 Element Plus Notification 显示错误

### 9.2 类型安全

- 前端 Interface 严格对齐后端 DTO
- 使用 TypeScript 泛型确保类型安全
- API 调用返回类型明确

### 9.3 响应式设计

- 桌面优先（复杂数据表格）
- 移动端：侧边栏变抽屉，表格变卡片视图

### 9.4 可访问性

- 图表使用高对比度配色
- 支持键盘导航
- 符合 WCAG 2.1 AA 标准

---

## 10. 开发规范

### 10.1 命名规范

- 组件名：PascalCase（如 `ModelList.vue`）
- 文件名：kebab-case（如 `use-chart.ts`）
- 变量名：camelCase（如 `modelList`）

### 10.2 代码规范

- 使用 ESLint 和 Prettier
- 遵循 Vue 3 Composition API 风格
- 使用 TypeScript 严格模式

### 10.3 Git 提交规范

- 遵循 Conventional Commits
- 格式：`type(scope): subject`
- 示例：`feat(model): 实现模型列表页面`

---

**计划生成时间**：2026-01-27
**预计完成时间**：根据实际开发进度调整
