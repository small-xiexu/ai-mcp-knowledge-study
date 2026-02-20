# AI MCP Knowledge Web

AI 多模型编排管理系统 - 前端项目

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Vue Router
- Pinia
- Axios
- ECharts

## 项目结构

```
src/
├── api/                  # API 接口定义
├── assets/               # 静态资源
├── components/           # 全局公共组件
│   ├── Layout/           # 布局组件
│   ├── JsonViewer/       # JSON 展示组件
│   └── StatusTag/        # 状态标签组件
├── hooks/                # 组合式函数
├── router/               # 路由配置
├── store/                # Pinia 状态管理
├── types/                # TypeScript 类型定义
├── utils/                # 工具库
└── views/                # 页面视图
    ├── dashboard/        # 仪表盘
    ├── model/            # 模型管理
    ├── audit/            # 审计日志
    └── playground/       # AI 调试演练场
```

## 开发指南

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

### 代码检查

```bash
npm run lint
```

### 代码格式化

```bash
npm run format
```

## 功能模块

### 1. 监控看板
- 系统健康状态
- 成功率统计
- 响应时间趋势
- 模型使用分布

### 2. 模型管理
- 模型列表（分页）
- 新增/编辑模型
- 启用/禁用模型
- 模型能力配置

### 3. 审计日志
- 调用日志查询
- TraceID 搜索
- JSON 详情查看

### 4. 调试演练
- AI 对话测试
- 模型选择

## API 接口

后端 API 基础 URL: `http://localhost:8080/api`

### 模型管理
- `GET /models` - 查询模型列表
- `GET /models/{id}` - 查询单个模型
- `POST /models` - 创建模型
- `PUT /models/{id}` - 更新模型
- `DELETE /models/{id}` - 删除模型
- `PUT /models/{id}/enable` - 启用模型
- `PUT /models/{id}/disable` - 禁用模型

### AI 调用
- `POST /ai/chat` - 通用 AI 调用
- `GET /ai/models/available` - 获取可用模型列表

### 监控统计
- `GET /metrics/calls` - 调用次数统计
- `GET /metrics/success-rate` - 成功率统计
- `GET /metrics/response-time` - 响应时间统计
- `GET /metrics/model-usage` - 模型使用分布

### 审计日志
- `GET /audit/logs` - 查询审计日志

## 开发规范

### 命名规范
- 组件名：PascalCase（如 `ModelList.vue`）
- 文件名：kebab-case（如 `use-chart.ts`）
- 变量名：camelCase（如 `modelList`）

### 代码规范
- 使用 ESLint 和 Prettier
- 遵循 Vue 3 Composition API 风格
- 使用 TypeScript 严格模式

### Git 提交规范
- 遵循 Conventional Commits
- 格式：`type(scope): subject`
- 示例：`feat(model): 实现模型列表页面`

## 许可证

MIT
