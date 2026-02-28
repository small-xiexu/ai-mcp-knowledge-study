# MCP Dynamic 10 分钟最小可跑通实操

> 目标：你不需要先啃源码，也能把 Dynamic MCP 主链路先跑起来。  
> 成功标准：配置启用后，连接刷新成功且 `running=true`，并且能查到工具清单。

## 0. 先准备 4 项

1. 已登录管理后台，且有 `tool:read`、`tool:write` 权限。  
2. 能打开页面「MCP 工具配置」。  
3. 你有一套可连接目标（二选一）：
- 方案 A（推荐新手）：本地 `STDIO`，使用 `npx` 跑示例 MCP Server。
- 方案 B：已有远程 `SSE/HTTP` MCP Server 地址。
4. 如果要用命令行验证工具清单，准备好登录 token（`tokenName` + `tokenValue`）。

## 1. 一张图先看懂主链路

```mermaid
flowchart LR
    A[新增 MCP 配置] --> B[点击启用]
    B --> C[点击 开启连接 / 重启所有连接]
    C --> D[运行时建连 initialize]
    D --> E[刷新工具回调]
    E --> F[running 为 true]
    F --> G[工具目录接口可查询到工具]
```

## 2. 10 分钟跑通（推荐：界面操作）

### 步骤 1：新增一条 Dynamic MCP 配置
1. 进入「MCP 工具配置」页面。  
2. 点击「新增 MCP」。  
3. 填写：
- 名称：`filesystem-stdio-demo`
- 类型：`STDIO`
- 启用状态：打开
- JSON 配置（直接粘贴）：

```json
{
  "command": "npx",
  "args": [
    "-y",
    "@modelcontextprotocol/server-filesystem",
    "/Users/你的用户名"
  ],
  "env": {
    "NODE_ENV": "production"
  }
}
```

4. 点「确定」保存。

说明：
- 如果你机器没装 Node.js / npx，请改用你现有的 `SSE/HTTP` MCP 地址。

### 步骤 2：把“配置态”变成“运行态”
1. 在刚创建那一行点「开启连接」。  
2. 或者点顶部「重启所有连接」。  
3. 再点「刷新列表」。

你应看到：
- 启用列是“启用”。
- 运行中列是“运行中”（即 `running=true`）。

### 步骤 3：失败时先看这 3 个点
1. 还是“未运行”：
- 先确认 JSON 配置能被解析（命令、参数格式正确）。  
2. 开启连接报错：
- 多半是命令不可执行（比如本机没有 `npx`）。  
3. 启用了但不生效：
- 你可能只做了“启用”，还没点“开启连接/重启所有连接”。

## 3. 5 分钟验证“工具真的进来了”

### 方式 A（推荐）：调用工具目录接口
先准备变量：

```bash
export BASE_URL="http://127.0.0.1:8091"
export TOKEN_NAME="请替换成登录 tokenName"
export TOKEN_VALUE="请替换成登录 tokenValue"
```

查询工具清单：

```bash
curl -sS -X POST "${BASE_URL}/api/mcp/tools/list" \
  -H "Content-Type: application/json" \
  -H "${TOKEN_NAME}: ${TOKEN_VALUE}"
```

成功信号：
- 返回 `code=200`，`data` 里有工具项（含 `toolKey/name/source`）。

### 方式 B：查看配置运行状态（可选）

```bash
curl -sS -X POST "${BASE_URL}/api/mcp/servers/list" \
  -H "Content-Type: application/json" \
  -H "${TOKEN_NAME}: ${TOKEN_VALUE}" \
  -d '{
    "pageNum": 1,
    "pageSize": 20
  }'
```

成功信号：
- 目标配置记录的 `running` 字段为 `true`。

## 4. 常见报错翻译（小白版）

1. “命令执行失败 / 找不到命令”
- 说明：本地没有安装对应命令（常见是 `npx`）。  
- 处理：安装 Node.js，或改用远程 `SSE/HTTP` 配置。

2. “刷新成功但 running=false”
- 说明：配置保存成功，但运行时初始化失败。  
- 处理：先检查 endpoint/command，再次“开启连接”。

3. “工具清单为空”
- 说明：目标 MCP Server 本身没有暴露工具，或连接未真正建立。  
- 处理：先确认 `running=true`，再检查对端是否支持 `tools/list`。

## 5. 跑通后下一步

1. 回主文档继续看 Dynamic 原理：
- `docs/MCP-从0到1实战指南.md` 第 2-7 章。  
2. 再进入 Gateway 章节前，建议先跑：
- `docs/MCP-Gateway-10分钟最小可跑通实操.md`。
