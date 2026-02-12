-- ============================================================
-- Gateway 融合迁移脚本
-- 说明：将 gateway 项目的 5 张核心表迁移到 knowledge 库，
--       并新增 mcp_tool_binding 工具绑定关系表
-- 执行前提：在 knowledge 所在的 MySQL 库中执行
-- ============================================================

-- 1. 网关实例表
CREATE TABLE IF NOT EXISTS mcp_gateway (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '网关唯一标识（业务ID）',
    gateway_name    VARCHAR(100) NOT NULL COMMENT '网关名称',
    gateway_desc    VARCHAR(500)          COMMENT '网关描述',
    gateway_version VARCHAR(20)           COMMENT '网关版本号',
    gateway_instructions TEXT              COMMENT '网关使用说明（供模型参考）',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_gateway_id (gateway_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 网关实例表';

-- 2. 网关认证表（供外部 SSE 路径鉴权使用）
CREATE TABLE IF NOT EXISTS mcp_gateway_auth (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '网关唯一标识',
    api_key         VARCHAR(128) NOT NULL COMMENT 'API Key',
    rate_limit      INT DEFAULT 100       COMMENT '速率限制（次/分钟）',
    expire_time     DATETIME              COMMENT '过期时间，NULL 表示永不过期',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_gateway_api_key (gateway_id, api_key),
    INDEX idx_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 网关认证表';

-- 3. 工具注册表（定义 HTTP 工具的调用配置）
CREATE TABLE IF NOT EXISTS mcp_tool_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_name       VARCHAR(100) NOT NULL COMMENT '工具名称（MCP tool name）',
    tool_description VARCHAR(500)         COMMENT '工具描述（供模型理解用途）',
    http_url        VARCHAR(500) NOT NULL COMMENT '目标 HTTP 接口地址',
    http_method     VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE/PATCH',
    http_headers    TEXT                  COMMENT '自定义请求头（JSON 对象）',
    timeout         INT DEFAULT 30000     COMMENT '超时时间（毫秒）',
    retry_times     INT DEFAULT 0         COMMENT '重试次数',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_gateway_tool (gateway_id, tool_name),
    INDEX idx_gateway_id (gateway_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具注册表';

-- 4. 参数映射表（通过 parent_id 构建嵌套树形结构）
CREATE TABLE IF NOT EXISTS mcp_tool_mapping (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '所属工具ID',
    mapping_type    VARCHAR(10)  NOT NULL COMMENT '映射类型：request/response',
    parent_id       BIGINT                COMMENT '父节点ID，NULL 表示根节点',
    field_name      VARCHAR(100) NOT NULL COMMENT '字段名称',
    mcp_type        VARCHAR(20)  NOT NULL COMMENT 'MCP 类型：string/number/boolean/object/array',
    mcp_desc        VARCHAR(500)          COMMENT '字段描述（供模型理解）',
    is_required     TINYINT(1) DEFAULT 0  COMMENT '是否必填：1-是 0-否',
    item_type       VARCHAR(20)           COMMENT 'array 元素类型',
    item_ref_id     BIGINT                COMMENT 'array 元素引用的 object 节点ID',
    http_path       VARCHAR(200)          COMMENT 'HTTP 参数路径（如 company.name）',
    http_location   VARCHAR(10)           COMMENT '参数位置：body/query/path/header',
    sort_order      INT DEFAULT 0         COMMENT '排序序号',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_mapping_node (tool_id, mapping_type, parent_id, field_name),
    INDEX idx_tool_id (tool_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具参数映射表';

-- 5. Schema 缓存表（避免每次 tools/list 重复生成 JSON Schema）
CREATE TABLE IF NOT EXISTS mcp_tool_schema (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '所属工具ID',
    schema_version  INT DEFAULT 1         COMMENT 'Schema 版本号',
    input_schema    TEXT                  COMMENT '输入 Schema（JSON）',
    output_schema   TEXT                  COMMENT '输出 Schema（JSON）',
    schema_hash     VARCHAR(64)           COMMENT 'Schema 内容的 SHA-256 哈希',
    is_active       TINYINT(1) DEFAULT 1  COMMENT '是否为当前活跃版本',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tool_schema_version (gateway_id, tool_id, schema_version),
    INDEX idx_tool_active (tool_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具 Schema 缓存表';

-- 6. 工具绑定关系表（控制工具对模型/会话的可见性）
CREATE TABLE IF NOT EXISTS mcp_tool_binding (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '工具ID',
    bind_type       VARCHAR(10)  NOT NULL COMMENT '绑定类型：MODEL/SESSION',
    bind_target_id  BIGINT       NOT NULL COMMENT '绑定目标ID（模型ID 或 会话ID）',
    enabled         TINYINT(1) DEFAULT 1  COMMENT '是否启用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_binding (tool_id, bind_type, bind_target_id),
    INDEX idx_bind_target (bind_type, bind_target_id),
    INDEX idx_gateway_id (gateway_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具绑定关系表';
