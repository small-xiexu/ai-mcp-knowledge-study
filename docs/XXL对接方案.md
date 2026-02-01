# XXL 对接方案（后端代理 + 前端定制）

## 1. 目标与范围

### 目标
- 统一体验：在现有 Vue3 项目内提供任务管理能力，不跳转外部系统
- 权限收敛：由本系统控制“可查看/可编辑”的最小权限
- 可控扩展：后续可接入审计、告警、审批等业务能力

### 范围（当前版本）
- 只管理指定执行器：appName = ai-rag-knowledge-executor
- 能力包含：任务列表、任务创建/修改、日志查看（按任务 + 时间）

## 2. 现状与约束

- 前端：Vue3 + Vite + Element Plus（ai-mcp-knowledge-web）
- 后端：Spring Boot 3.4.3（ai-mcp-knowledge-app）
- 认证与权限：当前无，先做“最小认证/权限”
- xxl-admin：Docker 运行，端口 9090，账号 admin/123456
- 版本：kuschzzp/xxl-job-aarch64:2.4.0

## 3. 方案选择结论

- 采用“前端定制 + 后端代理 xxl 接口”的方案
- 不采用 iframe 内嵌：无法满足统一体验与权限收敛

## 4. 总体架构

### 流程
1. 前端调用本系统接口（/api/xxl/…）
2. 后端通过 WebClient 代理调用 xxl-admin
3. 后端维护 xxl 登录态（cookie），使用 Redis 存储
4. 后端返回统一格式结果给前端

### 权限控制
- 仅两类权限：任务查看 / 任务编辑
- 后端强制校验，前端仅做 UI 级展示控制

## 5. Redis 登录态方案

### 目标
- 多实例共享登录态
- 宕机重启后无需频繁重新登录

### 设计
- 登录成功后，保存 cookie 到 Redis
- 每次调用 xxl 前加载 cookie
- 如果 cookie 失效：重新登录并更新 Redis

### 建议参数
- 缓存键：xxl:admin:cookie
- 过期时间：与 xxl session 一致或略短

## 6. 后端接口设计（全 POST）

### 对外接口（本系统）
- POST /api/xxl/jobs/list  
  入参：appName, page, size  
  出参：任务分页列表

- POST /api/xxl/job/detail  
  入参：id  
  出参：任务详情

- POST /api/xxl/job/create  
  入参：任务字段（见第 7 节）  
  出参：创建结果

- POST /api/xxl/job/update  
  入参：id + 任务字段  
  出参：更新结果

- POST /api/xxl/logs/list  
  入参：jobId, start, end, page, size  
  出参：日志分页列表

### 内部调用（xxl-admin）
- jobinfo/pageList
- jobinfo/loadById
- jobinfo/add
- jobinfo/update
- joblog/pageList

## 7. 字段范围与白名单

### 任务创建/修改可编辑字段
- scheduleConf（CRON 表达式）
- executorParam（执行参数）
- executorRouteStrategy（路由策略）
- executorHandler（执行器 handler）
- executorBlockStrategy（阻塞策略）
- timeout / failRetryCount 等可控字段

### 固定字段
- appName 固定为 ai-rag-knowledge-executor
- 其他敏感字段（如地址、注册信息）不开放给前端

## 8. WebClient 使用要点

### 数据格式
- xxl-admin 接口多为表单提交
- 需设置 content-type 为 application/x-www-form-urlencoded

### 会话管理
- 请求前附加 cookie
- 若响应表明未登录，触发重新登录

### 可靠性
- 配置超时
- 失败重试一次（仅限登录失效场景）

## 9. 前端页面规划

### 页面结构
- 任务中心（侧边栏入口）
- 任务列表
- 任务编辑（新建/修改共用）
- 日志查看

### 状态管理
- 列表查询与分页
- 表单校验与提交
- 日志查询与时间筛选

## 10. 迭代计划

### 第一阶段（最小可用）
- 后端打通登录 + 列表 + 日志
- 前端完成任务列表 + 日志页

### 第二阶段（可编辑）
- 后端支持创建/更新
- 前端完成任务表单

### 第三阶段（权限最小化）
- 引入最小认证
- 增加查看/编辑权限判断

## 11. 风险与注意事项

- xxl-admin 版本升级可能影响接口行为
- 表单字段需做严格白名单，避免越权字段
- 登录态失效必须可自动重登

## 12. 需要确认的配置

- xxl-admin 地址：http://localhost:9090
- xxl 账号：admin / 123456
- 执行器 appName：ai-rag-knowledge-executor
