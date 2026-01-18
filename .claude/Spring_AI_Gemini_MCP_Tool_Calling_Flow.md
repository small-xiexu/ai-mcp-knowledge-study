# Spring AI Gemini 模型 MCP 工具调用完整执行流程

> 本文档详细梳理了 `test_gemini_tool()` 测试方法中，从用户输入到最终输出的完整执行流程。

## 1. 整体架构概览

### 1.1 核心组件架构图

```mermaid
flowchart TB
    subgraph Application["应用层 Application Layer"]
        MCPTest["MCPTest.test_gemini_tool()"]
        GeminiModel["GoogleGenAiChatModel<br/>(Gemini 模型)"]
        ToolProvider["ToolCallbackProvider<br/>(MCP 工具提供者)"]
    end

    subgraph SpringAI["Spring AI 核心层"]
        ChatClient["ChatClient"]
        DefaultChatClient["DefaultChatClient<br/>(默认实现)"]
        ToolCallingManager["ToolCallingManager<br/>(工具调用管理器)"]
        ToolPredicate["ToolExecutionEligibilityPredicate<br/>(工具执行资格判断器)"]
    end

    subgraph MCPClient["MCP 客户端层"]
        SyncProvider["SyncMcpToolCallbackProvider"]
        McpClient["McpSyncClient<br/>(MCP 同步客户端)"]
        ToolCallbacks["ToolCallback[]<br/>(工具回调数组)"]
    end

    subgraph MCPServer["MCP 服务端层 (外部进程)"]
        FileSystem["@modelcontextprotocol/<br/>server-filesystem"]
        Tools["read_file, write_file,<br/>edit_file, ..."]
        STDIO["STDIO 协议通信"]
    end

    MCPTest --> GeminiModel
    MCPTest --> ToolProvider
    GeminiModel --> ChatClient
    ToolProvider --> ChatClient
    ChatClient --> DefaultChatClient
    DefaultChatClient --> ToolCallingManager
    ToolCallingManager --> ToolPredicate
    ToolProvider --> SyncProvider
    SyncProvider --> McpClient
    SyncProvider --> ToolCallbacks
    McpClient --> FileSystem
    FileSystem --> Tools
    FileSystem --> STDIO
```

### 1.2 依赖关系

| 组件 | Maven 依赖 | 作用 |
|------|-----------|------|
| Gemini 模型 | `spring-ai-starter-model-google-genai` | 提供 Gemini AI 模型能力 |
| MCP 客户端 | `spring-ai-starter-mcp-client-webflux` | 连接 MCP Server |
| MCP 服务端 | `spring-ai-starter-mcp-server` | 暴露本地工具服务 |

---

## 2. 启动阶段：Spring Boot 自动配置

### 2.1 MCP 客户端自动配置流程

```mermaid
flowchart TD
    A["应用启动"] --> B["Spring Boot Auto-Configuration"]

    B --> C["1. 读取 application-dev.yml 配置<br/>spring.ai.mcp.client.stdio.servers-configuration"]

    C --> D["2. 解析 mcp-servers-config.json"]

    D --> E["JSON 配置内容"]
    E --> |"mcpServers.filesystem"| F["command: npx<br/>args: -y @modelcontextprotocol/server-filesystem..."]

    F --> G["3. 创建 McpSyncClient 实例"]
    G --> G1["启动子进程: npx -y @modelcontextprotocol/server-filesystem"]
    G1 --> G2["建立 STDIO 通信管道"]

    G2 --> H["4. 创建 SyncMcpToolCallbackProvider Bean"]
    H --> H1["调用 McpSyncClient.listTools() 获取工具列表"]
    H1 --> H2["将每个 MCP Tool 转换为 ToolCallback"]

    H2 --> I["5. 注册 ToolCallbackProvider Bean 到 Spring 容器"]

    I --> J["配置完成 ✓"]
```

### 2.2 MCP 工具发现时序图

当 `McpSyncClient` 初始化时，会通过 MCP 协议向服务端发送请求：

```mermaid
sequenceDiagram
    participant App as Spring AI Application
    participant MCP as MCP Server (filesystem)

    App->>MCP: initialize request
    MCP-->>App: initialize response

    App->>MCP: tools/list request
    MCP-->>App: tools/list response

    Note right of MCP: 返回工具列表:<br/>read_file<br/>write_file<br/>edit_file<br/>create_directory<br/>list_directory<br/>directory_tree<br/>move_file<br/>search_files<br/>get_file_info<br/>read_multiple_files<br/>list_allowed_directories
```

### 2.3 返回的工具列表

| 工具名称 | 功能描述 |
|---------|----------|
| `read_file` | 读取文件的完整内容 |
| `read_multiple_files` | 同时读取多个文件的内容 |
| `write_file` | 建立新文件或完全覆写现有文件 |
| `edit_file` | 对文本文件进行基于行的编辑 |
| `create_directory` | 建立新目录或确保目录存在 |
| `list_directory` | 取得指定路径中所有文件和目录的详细清单 |
| `directory_tree` | 取得文件和目录的递归树状检视 |
| `move_file` | 移动或重新命名文件和目录 |
| `search_files` | 递归搜索符合模式的文件和目录 |
| `get_file_info` | 检索有关文件或目录的详细元数据 |
| `list_allowed_directories` | 返回此服务器允许访问的目录清单 |

---

## 3. 测试执行阶段：完整调用流程

### 3.1 主时序图（本例：询问工具列表）

```mermaid
sequenceDiagram
    autonumber
    participant Test as MCPTest
    participant Client as ChatClient
    participant Provider as ToolCallbackProvider
    participant Gemini as Gemini Model
    participant MCP as MCP Server

    Test->>Client: ChatClient.builder(geminiChatModel)<br/>.defaultToolCallbacks(tools)<br/>.build()

    Test->>Client: prompt("有哪些工具可以使用")

    Client->>Provider: getToolCallbacks()
    Provider-->>Client: ToolCallback[]

    Note over Client: 构建带工具定义的 Prompt

    Client->>Gemini: call(Prompt + Tools Schema)

    Note over Gemini: 分析用户意图<br/>发现是元问题<br/>无需调用工具

    Gemini-->>Client: 文本响应<br/>(直接列出工具列表)

    Client-->>Test: .content()

    Note over Test: 输出: 您可以使用以下工具...
```

### 3.2 详细执行步骤

#### 步骤 1：创建 ChatClient 实例

```java
var chatClient = ChatClient.builder(geminiChatModel)
        .defaultToolCallbacks(tools)  // 注入 MCP 工具回调
        .build();
```

**内部处理流程：**

```mermaid
flowchart LR
    A["ChatClient.builder()"] --> B["创建 DefaultChatClientBuilder"]
    B --> C["defaultToolCallbacks(tools)"]
    C --> D["ToolCallbackProvider → ToolCallback[]"]
    D --> E["build()"]
    E --> F["生成 DefaultChatClient 实例"]
```

#### 步骤 2：发起 Prompt 请求

```java
chatClient.prompt(userInput).call().content()
```

**内部处理流程：**

```mermaid
flowchart TD
    A["prompt('有哪些工具可以使用')"]
    A --> B["DefaultChatClient.prompt()"]

    B --> C["创建 DefaultChatClientRequest 对象"]
    C --> C1["userText: '有哪些工具可以使用'"]
    C --> C2["toolCallbacks: [read_file, write_file, ...]"]
    C --> C3["chatModel: GoogleGenAiChatModel"]

    C1 & C2 & C3 --> D["DefaultChatClientRequest.call()"]

    D --> E["1. 构建 Prompt 对象"]
    E --> E1["包含用户消息"]
    E --> E2["包含工具定义 (JSON Schema 格式)"]

    E1 & E2 --> F["2. 调用 ChatModel.call(prompt)"]
    F --> G["发送到 Gemini API"]
    G --> H["等待响应"]

    H --> I{"3. 检查 hasToolCalls()"}
    I -->|"false"| J["4. 返回 ChatResponse"]
    I -->|"true"| K["执行工具调用循环"]
    K --> F
```

#### 步骤 3：Gemini API 请求构建

`GoogleGenAiChatModel` 将请求转换为 Gemini API 格式：

```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        { "text": "有哪些工具可以使用" }
      ]
    }
  ],
  "tools": [
    {
      "functionDeclarations": [
        {
          "name": "read_file",
          "description": "读取文件的完整内容",
          "parameters": {
            "type": "object",
            "properties": {
              "path": {
                "type": "string",
                "description": "文件路径"
              }
            },
            "required": ["path"]
          }
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7
  }
}
```

#### 步骤 4：Gemini 模型响应

由于用户问的是 "有哪些工具可以使用"，Gemini 模型并未发起工具调用，而是直接基于请求中的工具定义信息，生成了工具列表的文本描述：

```
您可以使用以下工具：

`read_file`: 讀取檔案的完整內容。
`read_multiple_files`: 同時讀取多個檔案的內容。
`write_file`: 建立新檔案或完全覆寫現有檔案。
...
```

---

## 4. 带工具执行的完整流程（假设场景）

如果用户输入的是需要实际调用工具的请求（如："读取 /Users/xiexu/Desktop 目录下的文件列表"），则会触发完整的工具调用流程：

### 4.1 工具调用时序图

```mermaid
sequenceDiagram
    autonumber
    participant Test as MCPTest
    participant Client as ChatClient
    participant Gemini as Gemini Model
    participant Provider as ToolCallbackProvider
    participant MCP as MCP Server

    Test->>Client: prompt("读取/Users/xiexu/Desktop目录")

    Client->>Gemini: 发送请求 (Prompt + Tools)

    Gemini-->>Client: 返回工具调用请求<br/>{"functionCall":{"name":"list_directory", "args":{...}}}

    rect rgb(255, 245, 238)
        Note over Client,MCP: 工具执行阶段
        Client->>Provider: 查找 ToolCallback("list_directory")
        Provider-->>Client: ToolCallback 实例

        Client->>Provider: ToolCallback.call(args)
        Provider->>MCP: tools/call (MCP 协议)
        MCP-->>Provider: 执行结果 ["file1.txt", "file2.pdf"...]
        Provider-->>Client: 工具执行结果
    end

    Client->>Gemini: 将工具结果发回<br/>{"functionResponse":{...}}

    Gemini-->>Client: 生成最终响应

    Client-->>Test: .content()
```

### 4.2 工具调用内部处理流程

```mermaid
flowchart TD
    A["ChatClient 收到 ChatResponse"] --> B{"chatResponse.hasToolCalls()?"}

    B -->|"true"| C["提取工具调用请求<br/>List<ToolCall> toolCalls"]

    C --> D["遍历 toolCalls"]

    D --> E["获取 toolName 和 arguments"]
    E --> F["查找对应的 ToolCallback"]
    F --> G["执行 callback.call(arguments)"]
    G --> H["调用 MCP Server"]
    H --> I["获取执行结果"]
    I --> J["将结果加入对话历史"]

    J --> K{"还有更多 toolCalls?"}
    K -->|"是"| D
    K -->|"否"| L["将工具结果发回 ChatModel"]

    L --> M["chatModel.call(newPrompt)"]
    M --> B

    B -->|"false"| N["返回最终响应<br/>chatResponse.getResult().getOutput().getText()"]
```

---

## 5. 核心类与接口

### 5.1 类图

```mermaid
classDiagram
    class ToolCallbackProvider {
        <<interface>>
        +getToolCallbacks() ToolCallback[]
    }

    class SyncMcpToolCallbackProvider {
        -McpSyncClient[] mcpClients
        -ToolCallback[] toolCallbacks
        +getToolCallbacks() ToolCallback[]
    }

    class ToolCallback {
        <<interface>>
        +getName() String
        +getDescription() String
        +getInputSchema() String
        +call(args) String
    }

    class McpSyncClient {
        +initialize()
        +listTools() List~Tool~
        +callTool(name, args) Object
    }

    class MCPServer {
        <<external process>>
        @modelcontextprotocol/server-filesystem
    }

    ToolCallbackProvider <|.. SyncMcpToolCallbackProvider : implements
    SyncMcpToolCallbackProvider --> ToolCallback : creates
    SyncMcpToolCallbackProvider --> McpSyncClient : uses
    McpSyncClient --> MCPServer : STDIO通信
```

### 5.2 核心配置类

```mermaid
flowchart TB
    subgraph AutoConfig["McpClientAutoConfiguration"]
        A["@ConditionalOnProperty<br/>spring.ai.mcp.client.stdio.servers-configuration"]
        A --> B["@Bean McpSyncClient"]
        B --> B1["1. 解析 mcp-servers-config.json"]
        B1 --> B2["2. 启动 MCP Server 子进程"]
        B2 --> B3["3. 建立 STDIO 通信"]
        B3 --> B4["4. 发送 initialize 请求"]

        C["@ConditionalOnProperty<br/>spring.ai.mcp.client.toolcallback.enabled=true"]
        C --> D["@Bean ToolCallbackProvider"]
        D --> D1["return new SyncMcpToolCallbackProvider(client)"]
    end
```

---

## 6. 配置文件说明

### 6.1 application-dev.yml

```yaml
spring:
  ai:
    mcp:
      client:
        request-timeout: 360s                                    # MCP 请求超时时间
        stdio:
          servers-configuration: classpath:/config/mcp-servers-config.json  # MCP 服务配置
    google:
      genai:
        api-key: ${GOOGLE_AI_API_KEY}                           # Google AI API Key
        chat:
          options:
            model: gemini-2.0-flash                              # 使用的模型
```

### 6.2 mcp-servers-config.json

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-filesystem@2025.3.28",
        "/Users/xiexu/Desktop",
        "/Users/xiexu/Desktop"
      ]
    }
  }
}
```

### 6.3 配置解析流程

```mermaid
flowchart LR
    A["application-dev.yml"] --> B["指向 mcp-servers-config.json"]
    B --> C["解析 mcpServers 配置"]
    C --> D["filesystem 服务配置"]
    D --> E["启动命令: npx"]
    D --> F["参数: -y @modelcontextprotocol/server-filesystem"]
    D --> G["允许访问目录: /Users/xiexu/Desktop"]
```

---

## 7. 关键概念总结

### 7.1 MCP 协议 (Model Context Protocol)

```mermaid
mindmap
  root((MCP 协议))
    定义
      Anthropic 定义的开放协议
      用于 AI 应用与外部工具/数据源交互
    传输方式
      STDIO 标准输入输出
      SSE Server-Sent Events
      HTTP
    核心能力
      Tools 工具调用
      Resources 资源读取
      Prompts 提示词模板
```

### 7.2 Function Calling / Tool Calling 流程

```mermaid
flowchart LR
    A["模型接收<br/>工具定义"] --> B["分析<br/>用户意图"]
    B --> C["生成工具<br/>调用请求"]
    C --> D["执行<br/>工具"]
    D --> E["将结果<br/>返回模型"]
    E --> F["生成<br/>最终响应"]
```

### 7.3 为什么本例没有触发实际工具调用？

```mermaid
flowchart TD
    A["用户输入: '有哪些工具可以使用'"] --> B{"这是什么类型的问题?"}

    B --> C["元问题<br/>(关于工具本身的问题)"]

    C --> D["Gemini 模型分析"]
    D --> E["发现请求中已包含<br/>完整的工具定义信息"]
    E --> F["无需调用工具<br/>即可回答"]
    F --> G["直接从 JSON Schema<br/>提取工具名称和描述"]
    G --> H["生成文本响应"]

    style C fill:#e1f5fe
    style H fill:#c8e6c9
```

---

## 8. 完整数据流图

```mermaid
flowchart TB
    subgraph User["用户层"]
        Input["用户输入:<br/>'有哪些工具可以使用'"]
        Output["最终输出:<br/>工具列表描述"]
    end

    subgraph Test["测试层"]
        MCPTest["MCPTest.test_gemini_tool()"]
    end

    subgraph SpringAI["Spring AI 层"]
        ChatClient["ChatClient"]
        ToolCallbackProvider["ToolCallbackProvider"]
        Prompt["Prompt 对象<br/>(用户消息 + 工具定义)"]
    end

    subgraph Model["模型层"]
        GeminiModel["GoogleGenAiChatModel"]
        GeminiAPI["Gemini 2.0 Flash API"]
    end

    subgraph MCP["MCP 层"]
        McpClient["McpSyncClient"]
        MCPServer["MCP Server<br/>(filesystem)"]
    end

    Input --> MCPTest
    MCPTest --> ChatClient
    ChatClient --> ToolCallbackProvider
    ToolCallbackProvider --> McpClient
    McpClient -.-> MCPServer
    ToolCallbackProvider --> Prompt
    ChatClient --> Prompt
    Prompt --> GeminiModel
    GeminiModel --> GeminiAPI
    GeminiAPI --> GeminiModel
    GeminiModel --> ChatClient
    ChatClient --> MCPTest
    MCPTest --> Output

    style Input fill:#fff3e0
    style Output fill:#e8f5e9
```

---

## 9. 参考资料

- [Spring AI 官方文档 - MCP Client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [Spring AI 官方文档 - Tools](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Model Context Protocol 规范](https://modelcontextprotocol.io/)
- [Google AI Gemini API 文档](https://ai.google.dev/gemini-api/docs)

---

**文档作者**: xiexu
**创建日期**: 2026-01-17
**适用版本**: Spring AI 1.1.2 / Spring Boot 3.4.3
