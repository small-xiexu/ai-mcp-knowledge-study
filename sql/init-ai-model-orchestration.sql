-- =====================================================
-- AI 模型编排平台 - 数据库初始化脚本
-- MySQL 8.0+ | utf8mb4
-- 作者：xiexu
-- 更新：2026-02-20
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_model_orchestration
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_model_orchestration;

-- =====================================================
-- 0) 清理旧表（确保可重复执行，得到一致结构）
-- =====================================================
DROP TABLE IF EXISTS agent_run_context;
DROP TABLE IF EXISTS approval_request;
DROP TABLE IF EXISTS workflow_node_run;
DROP TABLE IF EXISTS workflow_run_context;
DROP TABLE IF EXISTS workflow_run;
DROP TABLE IF EXISTS workflow_edge;
DROP TABLE IF EXISTS workflow_node;
DROP TABLE IF EXISTS workflow_version;
DROP TABLE IF EXISTS workflow;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS agent_schedule;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS agent_version;
DROP TABLE IF EXISTS agent;
DROP TABLE IF EXISTS ai_client_profile_step;
DROP TABLE IF EXISTS ai_client_profile;
DROP TABLE IF EXISTS agent_enhancer_binding;
DROP TABLE IF EXISTS agent_enhancer;
DROP TABLE IF EXISTS mcp_tool_binding;
DROP TABLE IF EXISTS mcp_tool_schema;
DROP TABLE IF EXISTS mcp_tool_mapping;
DROP TABLE IF EXISTS mcp_tool_registry;
DROP TABLE IF EXISTS mcp_gateway_auth;
DROP TABLE IF EXISTS mcp_gateway;
DROP TABLE IF EXISTS ai_rag_task;
DROP TABLE IF EXISTS ai_chat_message;
DROP TABLE IF EXISTS ai_chat_session;
DROP TABLE IF EXISTS ai_model_activation;
DROP TABLE IF EXISTS ai_mcp_server_config;
DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_model_config;
DROP TABLE IF EXISTS sys_audit_event;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;

-- =====================================================
-- A. 用户/权限（治理底座）
-- =====================================================

-- 1) 用户表
CREATE TABLE sys_user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username         VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    display_name     VARCHAR(100) NOT NULL COMMENT '显示名',
    email            VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    mobile           VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    password_hash    VARCHAR(255) NOT NULL COMMENT '密码Hash',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    is_super_admin   TINYINT NOT NULL DEFAULT 0 COMMENT '是否超级管理员：1是 0否',
    last_login_at    DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip    VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 2) 角色表
CREATE TABLE sys_role (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_code        VARCHAR(64)  NOT NULL COMMENT '角色编码（唯一）',
    role_name        VARCHAR(100) NOT NULL COMMENT '角色名称',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_code (role_code),
    KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 3) 权限表
CREATE TABLE sys_permission (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    permission_code  VARCHAR(64)  NOT NULL COMMENT '权限编码（唯一）',
    permission_name  VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type    VARCHAR(64)  NOT NULL COMMENT '资源类型',
    action           VARCHAR(64)  NOT NULL COMMENT '动作',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_permission_code (permission_code),
    KEY idx_sys_permission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 4) 用户-角色关系
CREATE TABLE sys_user_role (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    role_id          BIGINT NOT NULL COMMENT '角色ID',
    granted_by       BIGINT DEFAULT NULL COMMENT '授权人ID',
    granted_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_user (user_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

-- 5) 角色-权限关系
CREATE TABLE sys_role_permission (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id          BIGINT NOT NULL COMMENT '角色ID',
    permission_id    BIGINT NOT NULL COMMENT '权限ID',
    granted_by       BIGINT DEFAULT NULL COMMENT '授权人ID',
    granted_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
    KEY idx_sys_role_permission_role (role_id),
    KEY idx_sys_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

-- 6) 统一审计事件表
CREATE TABLE sys_audit_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    operator_id      BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_type    VARCHAR(32) NOT NULL COMMENT '主体类型：user/system',
    event_type       VARCHAR(64) NOT NULL COMMENT '事件类型',
    resource_type    VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id      VARCHAR(128) NOT NULL COMMENT '资源ID',
    action           VARCHAR(64) NOT NULL COMMENT '动作',
    request_id       VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    source_ip        VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
    user_agent       VARCHAR(500) DEFAULT NULL COMMENT 'UA',
    old_value        JSON DEFAULT NULL COMMENT '旧值快照',
    new_value        JSON DEFAULT NULL COMMENT '新值快照',
    result           TINYINT NOT NULL DEFAULT 1 COMMENT '执行结果：1成功 0失败',
    error_message    VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    cost_ms          BIGINT DEFAULT NULL COMMENT '耗时ms',
    occurred_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    KEY idx_sys_audit_operator_time (operator_id, occurred_at),
    KEY idx_sys_audit_resource (resource_type, resource_id),
    KEY idx_sys_audit_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审计事件表';

-- 初始化权限/角色/用户
SET @now = NOW();

INSERT INTO sys_permission (permission_code, permission_name, resource_type, action, status, created_at, updated_at)
VALUES
('user:read', '读取用户', 'user', 'read', 1, @now, @now),
('user:write', '编辑用户', 'user', 'write', 1, @now, @now),
('role:read', '读取角色', 'role', 'read', 1, @now, @now),
('role:write', '编辑角色', 'role', 'write', 1, @now, @now),
('audit:read', '读取审计', 'audit', 'read', 1, @now, @now),
('agent:read', '读取 Agent', 'agent', 'read', 1, @now, @now),
('agent:write', '编辑 Agent', 'agent', 'write', 1, @now, @now),
('agent:publish', '发布 Agent', 'agent', 'publish', 1, @now, @now),
('agent:invoke', '调用 Agent', 'agent', 'invoke', 1, @now, @now),
('workflow:read', '读取 Workflow', 'workflow', 'read', 1, @now, @now),
('workflow:write', '编辑 Workflow', 'workflow', 'write', 1, @now, @now),
('workflow:publish', '发布 Workflow', 'workflow', 'publish', 1, @now, @now),
('workflow:invoke', '调用 Workflow', 'workflow', 'invoke', 1, @now, @now),
('agent-enhancer:read', '读取 AgentEnhancer', 'agent-enhancer', 'read', 1, @now, @now),
('agent-enhancer:write', '编辑 AgentEnhancer', 'agent-enhancer', 'write', 1, @now, @now),
('tool:read', '读取工具', 'tool', 'read', 1, @now, @now),
('tool:write', '编辑工具', 'tool', 'write', 1, @now, @now),
('tool:invoke', '调用工具', 'tool', 'invoke', 1, @now, @now),
('tool:approve', '审批工具', 'tool', 'approve', 1, @now, @now),
('release:approve', '发布审批', 'release', 'approve', 1, @now, @now)
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
resource_type = VALUES(resource_type),
action = VALUES(action),
status = VALUES(status),
updated_at = VALUES(updated_at);

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
VALUES
('PLATFORM_ADMIN', '平台管理员', 1, '平台级全权限', @now, @now),
('BUSINESS_ADMIN', '业务管理员', 1, '业务管理权限', @now, @now),
('AGENT_OWNER', 'Agent负责人', 1, 'Agent 配置与发布权限', @now, @now),
('AUDITOR', '审计员', 1, '审计只读', @now, @now),
('VIEWER', '观察者', 1, '平台只读访问', @now, @now)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

-- 默认管理员账号（仅建议本地开发使用）
INSERT INTO sys_user (
    username, display_name, email, password_hash,
    status, is_super_admin, created_at, updated_at
)
VALUES (
    'admin', '平台管理员', 'admin@example.com',
    -- 默认密码：123456
    '$2y$10$cMMKYng7Nsk60ffdRUwP6eVu4oSYFymt37TAHYqcrUK.VPO.Y3Dn2',
    1, 1, @now, @now
)
ON DUPLICATE KEY UPDATE
display_name = VALUES(display_name),
email = VALUES(email),
status = VALUES(status),
is_super_admin = VALUES(is_super_admin),
updated_at = VALUES(updated_at);

-- 取关键 ID
SELECT id INTO @admin_user_id FROM sys_user WHERE username = 'admin' LIMIT 1;
SELECT id INTO @platform_admin_role_id FROM sys_role WHERE role_code = 'PLATFORM_ADMIN' LIMIT 1;
SELECT id INTO @business_admin_role_id FROM sys_role WHERE role_code = 'BUSINESS_ADMIN' LIMIT 1;
SELECT id INTO @agent_owner_role_id FROM sys_role WHERE role_code = 'AGENT_OWNER' LIMIT 1;
SELECT id INTO @auditor_role_id FROM sys_role WHERE role_code = 'AUDITOR' LIMIT 1;
SELECT id INTO @viewer_role_id FROM sys_role WHERE role_code = 'VIEWER' LIMIT 1;

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id, granted_by, granted_at)
VALUES
(@admin_user_id, @platform_admin_role_id, @admin_user_id, @now),
(@admin_user_id, @business_admin_role_id, @admin_user_id, @now)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 平台管理员：赋予全部权限
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @platform_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 业务管理员：核心管理权限（含工具审批）
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @business_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN (
    'user:read', 'user:write', 'role:read', 'role:write', 'audit:read',
    'agent:read', 'agent:write', 'agent:publish', 'agent:invoke',
    'workflow:read', 'workflow:write', 'workflow:publish', 'workflow:invoke',
    'agent-enhancer:read', 'agent-enhancer:write',
    'tool:read', 'tool:write', 'tool:invoke', 'tool:approve',
    'release:approve'
)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- Agent负责人：Agent 发布与调用 + 工具调用（不含审批）
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @agent_owner_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN (
    'agent:read', 'agent:write', 'agent:publish', 'agent:invoke',
    'workflow:read', 'workflow:write', 'workflow:publish', 'workflow:invoke',
    'agent-enhancer:read', 'agent-enhancer:write',
    'tool:read', 'tool:invoke',
    'audit:read'
)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 审计员：只读
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @auditor_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN ('audit:read', 'agent:read', 'workflow:read', 'agent-enhancer:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 观察者：只读
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @viewer_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN ('agent:read', 'workflow:read', 'agent-enhancer:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

SELECT 'Phase 1 用户体系初始化完成' AS message, NOW() AS executed_at;

-- =====================================================
-- B. 业务表
-- =====================================================

-- 1) 模型配置表
CREATE TABLE ai_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(50) NOT NULL COMMENT '模型类型(OPENAI/ANTHROPIC/GEMINI)',
    api_key VARCHAR(500) NOT NULL COMMENT 'API密钥',
    base_url VARCHAR(500) NOT NULL COMMENT 'API地址',
    completions_path VARCHAR(255) DEFAULT NULL COMMENT '对话补全路径（OpenAI兼容协议）',
    embeddings_path VARCHAR(255) DEFAULT NULL COMMENT '向量嵌入路径（OpenAI兼容协议）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    tool_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用工具调用(0:禁用 1:启用)',
    max_prompt_chars INT DEFAULT NULL COMMENT 'Prompt历史字符预算',
    max_history_messages INT DEFAULT NULL COMMENT 'Prompt历史消息条数预算',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_model_name (model_name),
    INDEX idx_type (model_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 2) 调用日志表（用于成本核算/指标聚合）
CREATE TABLE ai_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    request_content MEDIUMTEXT COMMENT '请求内容',
    response_content MEDIUMTEXT COMMENT '响应内容',
    response_time BIGINT DEFAULT 0 COMMENT '响应时间(ms)',
    status VARCHAR(20) NOT NULL COMMENT '状态(SUCCESS/FAILED/FALLBACK)',
    error_message MEDIUMTEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_created_at (created_at),
    INDEX idx_model (model_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

-- 3) MCP Server 配置表
CREATE TABLE ai_mcp_server_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    server_name VARCHAR(100) NOT NULL COMMENT 'MCP Server 名称',
    server_type VARCHAR(20) NOT NULL COMMENT 'MCP Server 类型(STDIO/HTTP/SSE/WEBSOCKET)',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    description VARCHAR(500) COMMENT '描述信息',
    command VARCHAR(255) COMMENT 'STDIO 命令',
    args TEXT COMMENT 'STDIO 参数(JSON数组)',
    env TEXT COMMENT 'STDIO 环境变量(JSON对象)',
    endpoint VARCHAR(500) COMMENT '远程服务地址',
    sse_endpoint VARCHAR(200) COMMENT 'SSE 连接路径',
    headers TEXT COMMENT 'HTTP Header(JSON对象)',
    connect_timeout_ms INT DEFAULT 10000 COMMENT '连接超时(毫秒)',
    request_timeout_ms INT DEFAULT 60000 COMMENT '请求超时(毫秒)',
    init_timeout_ms INT DEFAULT 60000 COMMENT '初始化超时(毫秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_mcp_server_name (server_name),
    INDEX idx_mcp_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Server 配置表';

-- 4) 模型激活配置表
CREATE TABLE ai_model_activation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    chat_model_id BIGINT COMMENT '当前激活的对话模型ID',
    embedding_model_id BIGINT COMMENT '当前激活的向量模型ID',
    singleton_key TINYINT NOT NULL DEFAULT 1 COMMENT '单例约束键（固定为1）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_model_activation_singleton (singleton_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型激活配置表';

-- 5) 聊天会话表（预留 agent 绑定）
CREATE TABLE ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    owner_user_id BIGINT DEFAULT NULL COMMENT '会话归属用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    model_id BIGINT COMMENT '会话默认模型ID',
    rag_tags TEXT COMMENT '关联知识库标签(JSON)',
    agent_id BIGINT DEFAULT NULL COMMENT 'Agent ID（多 Agent 平台）',
    agent_version_id BIGINT DEFAULT NULL COMMENT 'AgentVersion ID（多 Agent 平台）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_updated_at (updated_at),
    INDEX idx_updated_at_id (updated_at, id),
    INDEX idx_agent (agent_id),
    INDEX idx_agent_version (agent_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 6) 聊天消息表
CREATE TABLE ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)',
    content MEDIUMTEXT COMMENT '消息内容',
    model_id BIGINT COMMENT '实际使用的模型ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_session_id_id (session_id, id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 7) RAG 任务表
CREATE TABLE ai_rag_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    type VARCHAR(32) NOT NULL COMMENT '任务类型',
    status VARCHAR(32) NOT NULL COMMENT '任务状态',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    message VARCHAR(500) COMMENT '任务消息',
    rag_tag VARCHAR(100) COMMENT '知识库标签',
    error_details TEXT COMMENT '失败详情（JSON格式）',
    retry_count INT DEFAULT 0 COMMENT '任务级重试次数',
    parent_task_id VARCHAR(64) COMMENT '父任务ID（重试任务）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_status_retry (status, retry_count),
    INDEX idx_created_at (created_at),
    INDEX idx_status_updated_at (status, updated_at),
    INDEX idx_status_created_retry (status, created_at, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 任务表';

-- 8) MCP 网关实例表
CREATE TABLE mcp_gateway (
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

-- 9) MCP 网关认证表
CREATE TABLE mcp_gateway_auth (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '网关唯一标识',
    api_key         VARCHAR(255) NOT NULL COMMENT 'API Key',
    rate_limit      INT DEFAULT 100       COMMENT '速率限制（次/分钟）',
    expire_time     DATETIME              COMMENT '过期时间，NULL 表示永不过期',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_gateway_api_key (gateway_id, api_key),
    INDEX idx_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 网关认证表';

-- 10) MCP 工具注册表（补充 tool_key/risk_level）
CREATE TABLE mcp_tool_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_name       VARCHAR(100) NOT NULL COMMENT '工具名称（MCP tool name）',
    tool_key        VARCHAR(200) NOT NULL COMMENT '工具唯一键（gateway:{gatewayId}:{toolName} / mcp:{serverName}:{toolName}）',
    tool_description VARCHAR(500)         COMMENT '工具描述（供模型理解用途）',
    http_url        VARCHAR(500) NOT NULL COMMENT '目标 HTTP 接口地址',
    http_method     VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE/PATCH',
    http_headers    TEXT                  COMMENT '自定义请求头（JSON 对象）',
    timeout         INT DEFAULT 30000     COMMENT '超时时间（毫秒）',
    retry_times     INT DEFAULT 0         COMMENT '重试次数',
    risk_level      VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '风险等级：LOW/MEDIUM/HIGH',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_gateway_tool (gateway_id, tool_name),
    UNIQUE KEY uk_tool_key (tool_key),
    INDEX idx_gateway_id (gateway_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具注册表';

-- 11) MCP 工具参数映射表
CREATE TABLE mcp_tool_mapping (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '所属工具ID',
    mapping_type    VARCHAR(10)  NOT NULL COMMENT '映射类型：request/response',
    parent_id       BIGINT                COMMENT '父节点ID，NULL 表示根节点',
    parent_id_norm  BIGINT GENERATED ALWAYS AS (IFNULL(parent_id, 0)) STORED COMMENT '父节点归一化ID（根节点=0，用于唯一约束）',
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
    UNIQUE KEY uk_mapping_node (tool_id, mapping_type, parent_id_norm, field_name),
    INDEX idx_tool_id (tool_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具参数映射表';

-- 12) MCP 工具 Schema 缓存表
CREATE TABLE mcp_tool_schema (
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

-- 13) MCP 工具绑定关系表
CREATE TABLE mcp_tool_binding (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '工具ID',
    bind_type       VARCHAR(20)  NOT NULL COMMENT '绑定类型：MODEL/SESSION/AGENT_VERSION',
    bind_target_id  BIGINT       NOT NULL COMMENT '绑定目标ID',
    enabled         TINYINT(1) DEFAULT 1  COMMENT '是否启用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_binding (tool_id, bind_type, bind_target_id),
    INDEX idx_bind_target (bind_type, bind_target_id),
    INDEX idx_gateway_id (gateway_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具绑定关系表';

-- 14) AgentEnhancer 资产表
CREATE TABLE agent_enhancer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    agent_enhancer_code VARCHAR(64) NOT NULL COMMENT 'AgentEnhancer 唯一编码（单组织内唯一）',
    agent_enhancer_name VARCHAR(100) NOT NULL COMMENT 'AgentEnhancer 名称',
    agent_enhancer_type VARCHAR(30) NOT NULL COMMENT 'AgentEnhancer 类型（CHAT_MEMORY/REQUEST_RESPONSE_LOG/TOOL_CALL_LOG）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    config_json JSON DEFAULT NULL COMMENT '类型配置（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_agent_enhancer_code (agent_enhancer_code),
    INDEX idx_agent_enhancer_type (agent_enhancer_type),
    INDEX idx_agent_enhancer_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentEnhancer 资产表';

-- 15) AgentEnhancer 绑定关系表
CREATE TABLE agent_enhancer_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    bind_type VARCHAR(20) NOT NULL COMMENT '绑定类型：AGENT_VERSION/WORKFLOW_VERSION',
    bind_target_id BIGINT NOT NULL COMMENT '绑定目标ID',
    agent_enhancer_id BIGINT NOT NULL COMMENT 'AgentEnhancer ID',
    order_no INT DEFAULT 0 COMMENT '排序序号（越小越先执行）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_bind (bind_type, bind_target_id, agent_enhancer_id),
    INDEX idx_bind_target (bind_type, bind_target_id),
    INDEX idx_agent_enhancer (agent_enhancer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AgentEnhancer 绑定关系表';

-- =====================================================
-- C. 多 Agent 平台（控制面 + 运行面 + 审批）
-- =====================================================

-- 0) Client Profile（对齐 ai-agent-station 的客户端资产形态）
CREATE TABLE ai_client_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_code VARCHAR(64) NOT NULL COMMENT 'Client 编码（唯一）',
    client_name VARCHAR(100) NOT NULL COMMENT 'Client 名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_client_code (client_code),
    INDEX idx_client_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Client Profile 资产表';

CREATE TABLE ai_client_profile_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_profile_id BIGINT NOT NULL COMMENT 'Client Profile ID',
    sequence_no INT NOT NULL COMMENT '步骤顺序（越小越先执行）',
    step_name VARCHAR(100) DEFAULT NULL COMMENT '步骤名称',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    system_prompt MEDIUMTEXT DEFAULT NULL COMMENT '步骤系统提示词覆盖',
    enable_tools TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用工具',
    allowed_tool_keys_json JSON DEFAULT NULL COMMENT '步骤允许工具集合（toolKey JSON数组）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_client_profile_step_seq (client_profile_id, sequence_no),
    INDEX idx_client_profile_id (client_profile_id),
    INDEX idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Client Profile 步骤表';

-- 1) Agent
CREATE TABLE agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 对外唯一编码',
    agent_name VARCHAR(100) NOT NULL COMMENT 'Agent 名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    channel VARCHAR(32) NOT NULL DEFAULT 'agent' COMMENT '调用通道：agent/chat_stream',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    current_published_version_id BIGINT DEFAULT NULL COMMENT '当前发布版本ID',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_agent_code (agent_code),
    INDEX idx_status (status),
    INDEX idx_current_published_version (current_published_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 表';

-- 2) AgentVersion
CREATE TABLE agent_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    version_no INT NOT NULL COMMENT '版本号（递增）',
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT '变更摘要',
    prompt_template_id BIGINT DEFAULT NULL COMMENT '模板ID',
    prompt_template_version_no INT DEFAULT NULL COMMENT '模板版本号（发布固化）',
    template_params_json JSON DEFAULT NULL COMMENT '模板参数（JSON）',
    system_prompt_snapshot MEDIUMTEXT DEFAULT NULL COMMENT '系统提示词快照（发布固化）',
    workflow_version_id BIGINT DEFAULT NULL COMMENT '绑定 WorkflowVersion ID（可选；非空时 Agent 调用转 Workflow 执行）',
    output_contract_version VARCHAR(20) NOT NULL DEFAULT 'v1' COMMENT '输出契约版本',
    output_contract_options_json JSON DEFAULT NULL COMMENT '输出契约选项（JSON）',
    rag_mode VARCHAR(20) NOT NULL DEFAULT 'OPTIONAL' COMMENT 'RAG模式：DISABLED/OPTIONAL/REQUIRED',
    default_rag_tags_json JSON DEFAULT NULL COMMENT '默认RAG标签（JSON数组）',
    allowed_rag_tags_json JSON DEFAULT NULL COMMENT '允许覆盖RAG标签（JSON数组）',
    allowed_tool_keys_json JSON DEFAULT NULL COMMENT '允许工具集合（toolKey JSON数组）',
    client_profile_id BIGINT DEFAULT NULL COMMENT 'Client Profile ID（优先于 client_chain_json）',
    client_chain_json JSON DEFAULT NULL COMMENT '客户端串联步骤配置（JSON数组，按 sequence 顺序执行）',
    planning_config_json JSON DEFAULT NULL COMMENT 'Planning 配置（JSON对象）',
    timeout_ms INT DEFAULT 60000 COMMENT '超时毫秒',
    max_turns INT DEFAULT 20 COMMENT '最大轮次',
    temperature DECIMAL(4,2) DEFAULT 0.70 COMMENT '温度',
    repair_retry_times INT DEFAULT 2 COMMENT '结构化修复重试次数',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_agent_version_no (agent_id, version_no),
    INDEX idx_agent_state (agent_id, state),
    INDEX idx_workflow_version (workflow_version_id),
    INDEX idx_client_profile (client_profile_id),
    INDEX idx_prompt_template (prompt_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 版本表';

-- 3) PromptTemplate（统一模板资产）
CREATE TABLE prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号',
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    content MEDIUMTEXT NOT NULL COMMENT '模板内容（含占位符）',
    variable_spec_json JSON DEFAULT NULL COMMENT '变量契约（JSON）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_code_version (template_code, version_no),
    INDEX idx_state (state),
    INDEX idx_template_code_state (template_code, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词模板资产';

-- 4) AgentSchedule（XXL）
CREATE TABLE agent_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    schedule_name VARCHAR(100) NOT NULL COMMENT '调度名称（同 Agent 下唯一）',
    description VARCHAR(500) DEFAULT NULL COMMENT '调度描述',
    cron VARCHAR(100) NOT NULL COMMENT 'Cron 表达式',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    xxl_job_id BIGINT DEFAULT NULL COMMENT 'XXL Job ID',
    payload_template_json JSON DEFAULT NULL COMMENT '调度入参模板（JSON）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_agent_schedule_name (agent_id, schedule_name),
    INDEX idx_agent_id (agent_id),
    INDEX idx_agent_enabled (agent_id, enabled),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 调度配置';

-- 5) AgentRun（runId=traceId）
CREATE TABLE agent_run (
    run_id VARCHAR(64) PRIMARY KEY COMMENT '运行ID（建议=traceId）',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent Code',
    agent_version_id BIGINT NOT NULL COMMENT 'AgentVersion ID',
    run_type VARCHAR(30) NOT NULL COMMENT '运行类型：CHAT_SYNC/CHAT_STREAM/XXL_JOB',
    trigger_source VARCHAR(30) NOT NULL COMMENT '触发来源：HTTP/XXL',
    operator_id BIGINT DEFAULT NULL COMMENT '触发人ID',
    operator_type VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '主体类型：user/system',
    session_id BIGINT DEFAULT NULL COMMENT '会话ID',
    status VARCHAR(30) NOT NULL COMMENT '状态：RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED',
    model_id_used BIGINT DEFAULT NULL COMMENT '实际使用模型ID',
    model_name_used VARCHAR(100) DEFAULT NULL COMMENT '实际使用模型名称',
    tool_call_count INT DEFAULT 0 COMMENT '工具调用次数',
    tool_denied_count INT DEFAULT 0 COMMENT '工具拒绝次数',
    repair_attempts INT DEFAULT 0 COMMENT '结构化修复次数',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时ms',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    INDEX idx_time (started_at),
    INDEX idx_agent_version (agent_id, agent_version_id),
    INDEX idx_agent_version_id (agent_version_id),
    INDEX idx_status (status),
    INDEX idx_status_started_at (status, started_at),
    INDEX idx_operator_started_at (operator_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 运行记录';

-- 6) Workflow（独立资产：支持拖拽画布 + DAG 编排 + 多模型混用）
CREATE TABLE workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workflow_code VARCHAR(64) NOT NULL COMMENT 'Workflow 对外唯一编码',
    workflow_name VARCHAR(100) NOT NULL COMMENT 'Workflow 名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    current_published_version_id BIGINT DEFAULT NULL COMMENT '当前发布版本ID',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_workflow_code (workflow_code),
    INDEX idx_status (status),
    INDEX idx_current_published_version (current_published_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 资产表';

CREATE TABLE workflow_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workflow_id BIGINT NOT NULL COMMENT 'Workflow ID',
    version_no INT NOT NULL COMMENT '版本号（递增）',
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT '变更摘要',
    graph_json MEDIUMTEXT DEFAULT NULL COMMENT '画布快照（nodes+edges+viewport）',
    default_config_json JSON DEFAULT NULL COMMENT '默认配置（JSON，可被节点继承覆盖）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_workflow_version_no (workflow_id, version_no),
    INDEX idx_workflow_state (workflow_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 版本表';

CREATE TABLE workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workflow_version_id BIGINT NOT NULL COMMENT 'WorkflowVersion ID',
    node_key VARCHAR(64) NOT NULL COMMENT '节点唯一key（版本内唯一）',
    node_type VARCHAR(30) NOT NULL COMMENT '节点类型：START/LLM/RAG_RETRIEVE/TOOL_CALL/IF/PARALLEL/JOIN/OUTPUT/END',
    node_name VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
    config_json JSON DEFAULT NULL COMMENT '节点配置（JSON）',
    position_x INT DEFAULT NULL COMMENT '画布x',
    position_y INT DEFAULT NULL COMMENT '画布y',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_version_node_key (workflow_version_id, node_key),
    INDEX idx_version_type (workflow_version_id, node_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 节点表';

CREATE TABLE workflow_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    workflow_version_id BIGINT NOT NULL COMMENT 'WorkflowVersion ID',
    source_key VARCHAR(64) NOT NULL COMMENT '源节点key',
    target_key VARCHAR(64) NOT NULL COMMENT '目标节点key',
    edge_type VARCHAR(20) NOT NULL DEFAULT 'DEFAULT' COMMENT '边类型：DEFAULT/TRUE/FALSE/CONDITION',
    condition_expr VARCHAR(500) DEFAULT NULL COMMENT '条件表达式（edge_type=CONDITION 时）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_version_source (workflow_version_id, source_key),
    INDEX idx_version_target (workflow_version_id, target_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 边表';

CREATE TABLE workflow_run (
    run_id VARCHAR(64) PRIMARY KEY COMMENT '运行ID（建议=traceId）',
    workflow_id BIGINT NOT NULL COMMENT 'Workflow ID',
    workflow_code VARCHAR(64) NOT NULL COMMENT 'Workflow Code',
    workflow_version_id BIGINT NOT NULL COMMENT 'WorkflowVersion ID',
    trigger_source VARCHAR(30) NOT NULL COMMENT '触发来源：HTTP/XXL/APPROVAL',
    operator_id BIGINT DEFAULT NULL COMMENT '触发人ID',
    operator_type VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '主体类型：user/system',
    session_id BIGINT DEFAULT NULL COMMENT '会话ID',
    status VARCHAR(30) NOT NULL COMMENT '状态：RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED',
    current_node_key VARCHAR(64) DEFAULT NULL COMMENT '当前节点key（便于恢复/排障）',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时ms',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_time (started_at),
    INDEX idx_workflow_version (workflow_id, workflow_version_id),
    INDEX idx_workflow_version_id (workflow_version_id),
    INDEX idx_status (status),
    INDEX idx_status_started_at (status, started_at),
    INDEX idx_operator_started_at (operator_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 运行记录';

CREATE TABLE workflow_run_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    run_id VARCHAR(64) NOT NULL COMMENT '运行ID',
    status VARCHAR(30) NOT NULL COMMENT '上下文状态：SAVED/RESUMED/EXPIRED',
    snapshot_json JSON NOT NULL COMMENT '可恢复上下文快照（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_run (run_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 运行上下文快照';

CREATE TABLE workflow_node_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    run_id VARCHAR(64) NOT NULL COMMENT '运行ID',
    node_key VARCHAR(64) NOT NULL COMMENT '节点key',
    node_type VARCHAR(30) NOT NULL COMMENT '节点类型',
    node_name VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
    status VARCHAR(30) NOT NULL COMMENT '状态：RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/SKIPPED',
    model_id_used BIGINT DEFAULT NULL COMMENT '实际使用模型ID（LLM）',
    model_name_used VARCHAR(100) DEFAULT NULL COMMENT '实际使用模型名称（LLM）',
    tool_call_count INT DEFAULT 0 COMMENT '工具调用次数',
    tool_denied_count INT DEFAULT 0 COMMENT '工具拒绝次数',
    input_digest VARCHAR(500) DEFAULT NULL COMMENT '输入摘要（脱敏/截断）',
    output_digest VARCHAR(500) DEFAULT NULL COMMENT '输出摘要（脱敏/截断）',
    output_text MEDIUMTEXT DEFAULT NULL COMMENT '输出原文（可选，限长/脱敏）',
    output_truncated TINYINT NOT NULL DEFAULT 0 COMMENT '是否截断：1是 0否',
    approval_request_id BIGINT DEFAULT NULL COMMENT '审批单ID（PENDING_APPROVAL 时）',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时ms',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    INDEX idx_run (run_id),
    INDEX idx_run_node (run_id, node_key),
    INDEX idx_time (started_at),
    INDEX idx_run_status (run_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Workflow 节点运行明细';

-- 7) 审批单（高风险工具默认生成审批单）
CREATE TABLE approval_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    approval_type VARCHAR(30) NOT NULL COMMENT '审批类型：TOOL_INVOKE',
    status VARCHAR(30) NOT NULL COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED',
    run_id VARCHAR(64) NOT NULL COMMENT '关联 runId',
    agent_id BIGINT DEFAULT NULL COMMENT 'Agent ID（Agent 场景）',
    agent_version_id BIGINT DEFAULT NULL COMMENT 'AgentVersion ID（Agent 场景）',
    workflow_id BIGINT DEFAULT NULL COMMENT 'Workflow ID（Workflow 场景）',
    workflow_version_id BIGINT DEFAULT NULL COMMENT 'WorkflowVersion ID（Workflow 场景）',
    node_key VARCHAR(64) DEFAULT NULL COMMENT '触发审批的节点key（Workflow 场景）',
    requester_id BIGINT DEFAULT NULL COMMENT '申请人ID',
    requester_type VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '主体类型：user/system',
    request_reason VARCHAR(500) DEFAULT NULL COMMENT '申请原因',
    approver_id BIGINT DEFAULT NULL COMMENT '审批人ID',
    decision_comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    decided_at DATETIME DEFAULT NULL COMMENT '审批时间',
    tool_key VARCHAR(200) NOT NULL COMMENT '工具key',
    risk_level VARCHAR(10) NOT NULL COMMENT '风险等级：LOW/MEDIUM/HIGH',
    arguments_snapshot_json JSON DEFAULT NULL COMMENT '入参快照（脱敏）',
    arguments_digest VARCHAR(500) DEFAULT NULL COMMENT '入参摘要',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_run (run_id),
    INDEX idx_tool (tool_key),
    INDEX idx_agent_id (agent_id),
    INDEX idx_agent_version_id (agent_version_id),
    INDEX idx_workflow_version_id (workflow_version_id),
    INDEX idx_workflow (workflow_id, workflow_version_id),
    INDEX idx_approver_status (approver_id, status),
    INDEX idx_status_expire (status, expire_at),
    INDEX idx_run_tool_status_expire_id (run_id, tool_key, status, expire_at, id),
    INDEX idx_status_expire_id (status, expire_at, id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审批单';

-- 8) Agent 运行上下文快照（用于审批通过后自动续跑）
CREATE TABLE agent_run_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    run_id VARCHAR(64) NOT NULL COMMENT '运行ID',
    status VARCHAR(30) NOT NULL COMMENT '上下文状态：SAVED/RESUMED/EXPIRED',
    snapshot_json JSON NOT NULL COMMENT '可恢复上下文快照（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_run (run_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 运行上下文快照';

-- =====================================================
-- D. 初始化数据
-- =====================================================

-- 初始化数据：模型配置（示例）
INSERT INTO ai_model_config (
    model_name, model_type, api_key, base_url, completions_path, embeddings_path, enabled, tool_enabled, max_prompt_chars, max_history_messages
)
VALUES
('GPT-4', 'OPENAI', 'sk-placeholder', 'http://127.0.0.1:8045', '/v1/chat/completions', '/v1/embeddings', 1, 1, 12000, 20),
('Claude-3.5-Sonnet', 'ANTHROPIC', 'sk-ant-placeholder', 'https://api.anthropic.com', '/v1/chat/completions', '/v1/embeddings', 1, 1, 12000, 20),
('Gemini-3-Flash', 'GEMINI', 'sk-placeholder', 'http://127.0.0.1:8045', '/v1/chat/completions', '/v1/embeddings', 1, 1, 12000, 20)
ON DUPLICATE KEY UPDATE
model_type = VALUES(model_type),
api_key = VALUES(api_key),
base_url = VALUES(base_url),
completions_path = VALUES(completions_path),
embeddings_path = VALUES(embeddings_path),
enabled = VALUES(enabled),
tool_enabled = VALUES(tool_enabled),
max_prompt_chars = VALUES(max_prompt_chars),
max_history_messages = VALUES(max_history_messages),
updated_at = VALUES(updated_at);

SELECT id INTO @gpt4_id FROM ai_model_config WHERE model_name = 'GPT-4' LIMIT 1;
SELECT id INTO @claude_id FROM ai_model_config WHERE model_name = 'Claude-3.5-Sonnet' LIMIT 1;
SELECT id INTO @gemini_id FROM ai_model_config WHERE model_name = 'Gemini-3-Flash' LIMIT 1;

-- 初始化数据：模型激活配置
INSERT INTO ai_model_activation (chat_model_id, embedding_model_id)
VALUES
(@gpt4_id, @gpt4_id)
ON DUPLICATE KEY UPDATE
chat_model_id = VALUES(chat_model_id),
embedding_model_id = VALUES(embedding_model_id),
updated_at = VALUES(updated_at);

-- 初始化数据：AgentEnhancer 资产
INSERT INTO agent_enhancer(agent_enhancer_code, agent_enhancer_name, agent_enhancer_type, enabled, config_json)
VALUES
('chat_memory', '对话记忆', 'CHAT_MEMORY', 1, JSON_OBJECT('maxMessages', 20, 'conversationIdFrom', 'SESSION_ID')),
('request_response_log', '请求响应日志', 'REQUEST_RESPONSE_LOG', 1, NULL),
('tool_call_log', '工具调用日志', 'TOOL_CALL_LOG', 1, NULL)
ON DUPLICATE KEY UPDATE
agent_enhancer_name = VALUES(agent_enhancer_name),
agent_enhancer_type = VALUES(agent_enhancer_type),
enabled = VALUES(enabled),
config_json = VALUES(config_json),
updated_at = VALUES(updated_at);

SELECT '数据库初始化完成' AS message, NOW() AS executed_at;
