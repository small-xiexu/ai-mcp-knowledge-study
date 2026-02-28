# MCP Gateway 10 分钟最小可跑通实操

> 目标：你不用先懂全部源码，也能在 10 分钟内把 Gateway MCP 的核心链路跑通。
> 结果标准：你能看到 `endpoint`、`ping`、`initialize`、`tools/list`、`tools/call` 的返回。

## 0. 你要先准备 3 个值

1. `BASE_URL`
- 你的服务地址，比如 `http://127.0.0.1:8091`。

2. `GATEWAY_ID`
- 比如 `gateway_001`（在网关管理页里可看到）。

3. `API_KEY`
- 该网关可用的 API Key（如果网关未启用鉴权，可留空）。

如果你不确定值从哪来，先问项目同学要一套可用的测试数据，再往下做。

## 1. 第一步：先建立 SSE 连接（终端 1）

在终端 1 执行：

```bash
export BASE_URL="http://127.0.0.1:8091"
export GATEWAY_ID="gateway_001"
export API_KEY="请替换成真实值"

curl -N \
  -H "Accept: text/event-stream" \
  -H "X-API-Key: ${API_KEY}" \
  "${BASE_URL}/api/gateway/${GATEWAY_ID}/mcp/sse"
```

你应该看到类似输出：

```text
event: endpoint
data: /api/gateway/gateway_001/mcp/message?sessionId=xxxx

event: ping
data: {}
```

说明：
1. `endpoint` 是后续发消息的地址（里面带 `sessionId`）。
2. `ping` 是心跳，表示连接还活着。

## 2. 第二步：发送 initialize（终端 2）

新开终端 2，把上一步 `endpoint` 的 `data` 值粘贴到 `MESSAGE_PATH`：

```bash
export BASE_URL="http://127.0.0.1:8091"
export MESSAGE_PATH="/api/gateway/gateway_001/mcp/message?sessionId=请替换"

curl -sS -X POST "${BASE_URL}${MESSAGE_PATH}" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "init-1",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": {
        "name": "quickstart-client",
        "version": "1.0.0"
      }
    }
  }'
```

成功信号：
1. 终端 2 返回 HTTP 200。
2. 终端 1 收到 `event: message`，内容里有 `protocolVersion/capabilities/serverInfo`。

## 3. 第三步：发送 tools/list（终端 2）

```bash
curl -sS -X POST "${BASE_URL}${MESSAGE_PATH}" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "list-1",
    "method": "tools/list",
    "params": {}
  }'
```

成功信号：
1. 终端 1 收到 `event: message`。
2. 数据里有 `result.tools` 数组，至少一个工具。

## 4. 第四步：发送 tools/call（终端 2）

先从 `tools/list` 返回里挑一个工具名，填到 `name`。
如果你暂时不清楚参数，先传空对象 `{}` 试一次，看错误提示也有价值。

```bash
curl -sS -X POST "${BASE_URL}${MESSAGE_PATH}" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "call-1",
    "method": "tools/call",
    "params": {
      "name": "请替换成 tools/list 返回的工具名",
      "arguments": {}
    }
  }'
```

返回判读：
1. 成功：`result.content[0].text` 有内容。
2. 参数问题：`error.code = -32602`。
3. 执行失败：`error.code = -32603`（通常是下游接口异常或超时）。

## 5. 3 分钟排错（最常见）

1. SSE 连不上
- 先查网关是否启用（状态必须启用）。
- 再查 API Key 是否正确/过期。

2. 有 `ping` 但没有 `message`
- 多半是 `MESSAGE_PATH` 里的 `sessionId` 贴错了。

3. tools/list 为空
- 多半是该网关下没有启用工具，或工具被可见性规则过滤。

4. tools/call 一直失败
- 看 `-32602` 还是 `-32603`，前者修参数，后者查下游服务。

## 6. 你已经跑通后，下一步看哪

1. 回到主文档：
- `docs/MCP-从0到1实战指南.md` 的“第二大块：Gateway MCP（协议入口与工具治理）”。

2. 按小白阅读顺序继续：
- `McpGatewayController` -> `GatewaySessionService` -> `GatewayMessageService` -> 三个 Handler -> `GatewayToolServiceImpl`。

## 7. 专项：接入 `mcp-tool-weixin`（HTTP 工具）

### 7.1 一句话结论
`mcp-tool-weixin` 按 HTTP 工具接入：走「HTTP 工具配置（Gateway）」，不要走「MCP 工具配置（Dynamic）」。

### 7.2 按页面一步一步配置（推荐）

1. 先启动 `mcp-tool-weixin` 服务，确认本机可访问 `http://127.0.0.1:8104`。  
可先用这个命令做连通性检查（返回成功或业务错误都说明服务已起来）：

```bash
curl -sS -X POST "http://127.0.0.1:8104/api/weixin/notice/send" \
  -H "Content-Type: application/json" \
  -d '{
    "platform":"测试平台",
    "subject":"连通性检查",
    "description":"Gateway 对接前检查",
    "jumpUrl":"https://example.com"
  }'
```

2. 打开本系统页面「HTTP 工具配置」：
- 新增网关实例（网关 ID 默认固定 `default_gateway`，状态设为启用）。
3. 进入该网关的「工具配置」，点「新增工具」，基础信息这样填：
- 工具名称：`sendWeixinNotice`
- 工具描述：`微信公众号回调工具`
- HTTP 方法：`POST`
- HTTP URL：`http://127.0.0.1:8104/api/weixin/notice/send`
- 超时：`30000`
- 重试次数：`0`
- 请求头 JSON：`{}`

4. 配“请求参数映射”：
- 点击「导入JSON」，粘贴：

```json
{
  "platform": "测试平台",
  "subject": "网关联调验证",
  "description": "SSE+tools/list+tools/call 集成测试",
  "jumpUrl": "https://example.com"
}
```

- 生成后检查每一行：
- 参数位置为 `请求体(body)`
- 字段名为 `platform/subject/description/jumpUrl`

5. 配“响应提取规则”（可选，但推荐）：
- 点击「导入JSON」，粘贴：

```json
{
  "success": true,
  "message": "发送成功",
  "errorCode": null
}
```

- 生成后可直接保存，或按需调整字段名。

6. 保存工具后，在工具行点「测试」，请求参数填：

```json
{
  "platform": "测试平台",
  "subject": "网关联调验证",
  "description": "来自 Gateway debug",
  "jumpUrl": "https://example.com"
}
```

成功信号：
1. 调试结果 `success=true`。  
2. 在 `tools/list` 能看到 `sendWeixinNotice`。  
3. `tools/call` 返回 `result.content`（失败时一般是 `-32603`）。

### 7.3 如果要给外部 MCP 客户端使用
1. 进入页面「凭证管理」给 `default_gateway` 新建一条 API Key（可自动生成）。  
2. 回到本文第 1-4 步联调时，把 `GATEWAY_ID` 改成 `default_gateway`，`API_KEY` 用刚创建的值。  

### 7.4 常见问题（这个专项最常见）
1. `tools/call` 返回 `-32603`
- 多半是 `mcp-tool-weixin` 没启动，或它自身微信配置不完整。  
2. `tools/list` 看不到 `sendWeixinNotice`
- 先检查工具状态是否启用，再确认网关状态是否启用。  
