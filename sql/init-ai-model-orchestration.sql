-- =====================================================
-- 全量初始化脚本（业务表 + 用户权限体系 + 组织隔离 + 多 Agent 平台治理）
-- 适用：MySQL 8.0+
-- 字符集：utf8mb4
-- 作者：xiexu
-- 更新：2026-02-14
--
-- 说明：
-- 1) 本脚本用于“从零初始化”或“作为备份参考”。
--    注意：会 DROP 现有表（清空数据）。请勿在生产库执行。
-- 2) 若需在旧库上升级，请优先使用迁移脚本：
--    sql/migrate-20260214-org-and-multi-agent-platform.sql
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_model_orchestration
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_model_orchestration;

-- =====================================================
-- 0) 清理旧表（确保可重复执行，得到一致结构）
-- =====================================================
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS agent_run_context;
DROP TABLE IF EXISTS approval_request;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS agent_schedule;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS agent_version;
DROP TABLE IF EXISTS agent;
DROP TABLE IF EXISTS tool_policy;
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
DROP TABLE IF EXISTS ai_config_audit;
DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_task_type;
DROP TABLE IF EXISTS ai_model_capability;
DROP TABLE IF EXISTS ai_model_config;
DROP TABLE IF EXISTS sys_audit_event;
DROP TABLE IF EXISTS sys_user_org;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_org;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- A. 用户/权限/组织（治理底座）
-- =====================================================

-- 1) 用户表
CREATE TABLE sys_user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username         VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    display_name     VARCHAR(100) NOT NULL COMMENT '显示名',
    email            VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    password_hash    VARCHAR(255) NOT NULL COMMENT '密码Hash',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    is_super_admin   TINYINT NOT NULL DEFAULT 0 COMMENT '是否超级管理员：1是 0否',
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
    role_scope       VARCHAR(20)  NOT NULL COMMENT '角色范围：PLATFORM/TENANT/GLOBAL',
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
    KEY idx_sys_user_role_role (role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
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
    KEY idx_sys_role_permission_permission (permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

-- 6) 组织表
CREATE TABLE sys_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_code         VARCHAR(64)  NOT NULL COMMENT '组织编码（唯一）',
    org_name         VARCHAR(100) NOT NULL COMMENT '组织名称',
    parent_id        BIGINT DEFAULT NULL COMMENT '父组织ID',
    org_path         VARCHAR(255) DEFAULT NULL COMMENT '组织路径',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_org_code (org_code),
    KEY idx_sys_org_parent (parent_id),
    CONSTRAINT fk_sys_org_parent FOREIGN KEY (parent_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统组织表';

-- 7) 用户-组织关系
CREATE TABLE sys_user_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    org_id           BIGINT NOT NULL COMMENT '组织ID',
    is_primary       TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织',
    joined_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_sys_user_org (user_id, org_id),
    KEY idx_sys_user_org_org (org_id),
    CONSTRAINT fk_sys_user_org_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_org_org FOREIGN KEY (org_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组织关系表';

-- 8) 统一审计事件表（同时记录操作人 org 与资源 org）
CREATE TABLE sys_audit_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    operator_id      BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_org_id  BIGINT NOT NULL COMMENT '操作人所属组织ID',
    operator_type    VARCHAR(32) NOT NULL COMMENT '主体类型：user/system',
    event_type       VARCHAR(64) NOT NULL COMMENT '事件类型',
    resource_type    VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id      VARCHAR(128) NOT NULL COMMENT '资源ID',
    resource_org_id  BIGINT NOT NULL COMMENT '资源归属组织ID',
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
    KEY idx_sys_audit_request_id (request_id),
    KEY idx_sys_audit_resource_org_time (resource_org_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审计事件表';

-- 初始化权限/角色/组织/用户
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

INSERT INTO sys_role (role_code, role_name, role_scope, status, remark, created_at, updated_at)
VALUES
('PLATFORM_ADMIN', '平台管理员', 'PLATFORM', 1, '平台级全权限', @now, @now),
('BUSINESS_ADMIN', '业务管理员', 'GLOBAL', 1, '业务管理权限', @now, @now),
('AGENT_OWNER', 'Agent负责人', 'GLOBAL', 1, 'Agent 配置与发布权限', @now, @now),
('AUDITOR', '审计员', 'TENANT', 1, '审计只读', @now, @now),
('VIEWER', '观察者', 'TENANT', 1, '平台只读访问', @now, @now)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
role_scope = VALUES(role_scope),
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
password_hash = VALUES(password_hash),
status = VALUES(status),
is_super_admin = VALUES(is_super_admin),
updated_at = VALUES(updated_at);

-- ROOT 组织（用于默认归属）
INSERT INTO sys_org (org_code, org_name, parent_id, org_path, status, remark, created_at, updated_at)
VALUES
('ROOT', '默认组织', NULL, '/ROOT', 1, '默认根组织', @now, @now)
ON DUPLICATE KEY UPDATE
org_name = VALUES(org_name),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

-- 取关键 ID
SELECT id INTO @admin_user_id FROM sys_user WHERE username = 'admin' LIMIT 1;
SELECT id INTO @root_org_id FROM sys_org WHERE org_code = 'ROOT' LIMIT 1;
SELECT id INTO @platform_admin_role_id FROM sys_role WHERE role_code = 'PLATFORM_ADMIN' LIMIT 1;
SELECT id INTO @tenant_admin_role_id FROM sys_role WHERE role_code = 'BUSINESS_ADMIN' LIMIT 1;
SELECT id INTO @agent_owner_role_id FROM sys_role WHERE role_code = 'AGENT_OWNER' LIMIT 1;
SELECT id INTO @auditor_role_id FROM sys_role WHERE role_code = 'AUDITOR' LIMIT 1;
SELECT id INTO @viewer_role_id FROM sys_role WHERE role_code = 'VIEWER' LIMIT 1;

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id, granted_by, granted_at)
VALUES
(@admin_user_id, @platform_admin_role_id, @admin_user_id, @now),
(@admin_user_id, @tenant_admin_role_id, @admin_user_id, @now)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 绑定管理员主组织
INSERT INTO sys_user_org (user_id, org_id, is_primary, joined_at)
VALUES
(@admin_user_id, @root_org_id, 1, @now)
ON DUPLICATE KEY UPDATE
is_primary = VALUES(is_primary),
joined_at = VALUES(joined_at);

-- 平台管理员：赋予全部权限
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @platform_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 业务管理员：核心管理权限（含工具审批）
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @tenant_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN (
    'user:read', 'user:write', 'role:read', 'role:write', 'audit:read',
    'agent:read', 'agent:write', 'agent:publish', 'agent:invoke',
    'workflow:read', 'workflow:write',
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
    'workflow:read', 'workflow:write',
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
WHERE p.permission_code IN ('audit:read', 'agent:read', 'workflow:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 观察者：只读
INSERT INTO sys_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT @viewer_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN ('agent:read', 'workflow:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

SELECT 'Phase 1 用户体系初始化完成' AS message, NOW() AS executed_at;

-- =====================================================
-- B. 业务表（全部按 org_id 隔离）
-- =====================================================

-- 1) 模型配置表（各部门自有）
CREATE TABLE ai_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(50) NOT NULL COMMENT '模型类型(OPENAI/ANTHROPIC/GEMINI)',
    api_key VARCHAR(500) NOT NULL COMMENT 'API密钥',
    base_url VARCHAR(500) NOT NULL COMMENT 'API地址',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    tool_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用工具调用(0:禁用 1:启用)',
    priority INT DEFAULT 0 COMMENT '优先级(数值越大越优先；用于默认/扩展策略排序，是否生效取决于策略实现)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_model_name (org_id, model_name),
    INDEX idx_org_type (org_id, model_type),
    INDEX idx_org_enabled (org_id, enabled),
    INDEX idx_org_priority (org_id, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表（按 org 隔离）';

-- 2) 模型能力表
CREATE TABLE ai_model_capability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    max_input_tokens INT DEFAULT 0 COMMENT '最大输入token',
    max_output_tokens INT DEFAULT 0 COMMENT '最大输出token',
    support_function_calling TINYINT(1) DEFAULT 0 COMMENT '支持函数调用',
    support_vision TINYINT(1) DEFAULT 0 COMMENT '支持视觉',
    support_streaming TINYINT(1) DEFAULT 1 COMMENT '支持流式输出',
    quality_score INT DEFAULT 50 COMMENT '质量评分(1-100)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (model_id) REFERENCES ai_model_config(id) ON DELETE CASCADE,
    UNIQUE KEY uk_org_model_id (org_id, model_id),
    INDEX idx_org_quality (org_id, quality_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型能力表（按 org 隔离）';

-- 3) 任务类型表（各部门自有）
CREATE TABLE ai_task_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编码',
    description TEXT COMMENT '任务描述',
    preferred_model_id BIGINT COMMENT '首选模型ID',
    fallback_model_ids VARCHAR(500) COMMENT '备用模型ID列表(逗号分隔)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_task_code (org_id, task_code),
    INDEX idx_org_task_name (org_id, task_name),
    FOREIGN KEY (preferred_model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务类型表（按 org 隔离）';

-- 4) 调用日志表（按 org 隔离，用于成本核算/指标聚合）
CREATE TABLE ai_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    task_type VARCHAR(50) COMMENT '任务类型',
    request_content TEXT COMMENT '请求内容',
    response_content TEXT COMMENT '响应内容',
    tokens_used INT DEFAULT 0 COMMENT '使用token数',
    response_time BIGINT DEFAULT 0 COMMENT '响应时间(ms)',
    status VARCHAR(20) NOT NULL COMMENT '状态(SUCCESS/FAILED/FALLBACK)',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_created_at (org_id, created_at),
    INDEX idx_org_model (org_id, model_id),
    INDEX idx_org_status (org_id, status),
    FOREIGN KEY (model_id) REFERENCES ai_model_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表（按 org 隔离）';

-- 5) 配置审计表（按 org 隔离）
CREATE TABLE ai_config_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID（资源归属 org）',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    record_id BIGINT NOT NULL COMMENT '记录ID',
    operation VARCHAR(20) NOT NULL COMMENT '操作(INSERT/UPDATE/DELETE)',
    old_value TEXT COMMENT '旧值(JSON)',
    new_value TEXT COMMENT '新值(JSON)',
    operator VARCHAR(100) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_table (org_id, table_name),
    INDEX idx_org_record (org_id, record_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计日志表（按 org 隔离）';

-- 6) MCP Server 配置表（按 org 隔离）
CREATE TABLE ai_mcp_server_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
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
    UNIQUE KEY uk_org_mcp_server_name (org_id, server_name),
    INDEX idx_org_mcp_enabled (org_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP Server 配置表（按 org 隔离）';

-- 7) 模型激活配置表（每 org 一条）
CREATE TABLE ai_model_activation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    chat_model_id BIGINT COMMENT '当前激活的对话模型ID',
    embedding_model_id BIGINT COMMENT '当前激活的向量模型ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_activation (org_id),
    FOREIGN KEY (chat_model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL,
    FOREIGN KEY (embedding_model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型激活配置表（按 org 隔离）';

-- 8) 聊天会话表（按 org 隔离，预留 agent 绑定）
CREATE TABLE ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    owner_user_id BIGINT DEFAULT NULL COMMENT '会话归属用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    model_id BIGINT COMMENT '会话默认模型ID',
    rag_tags TEXT COMMENT '关联知识库标签(JSON)',
    agent_id BIGINT DEFAULT NULL COMMENT 'Agent ID（多 Agent 平台）',
    agent_version_id BIGINT DEFAULT NULL COMMENT 'AgentVersion ID（多 Agent 平台）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org_updated_at (org_id, updated_at),
    INDEX idx_org_agent (org_id, agent_id),
    FOREIGN KEY (model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表（按 org 隔离）';

-- 9) 聊天消息表（按 org 隔离）
CREATE TABLE ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)',
    content TEXT COMMENT '消息内容',
    model_id BIGINT COMMENT '实际使用的模型ID',
    prompt_tokens INT DEFAULT 0 COMMENT '提示词token数',
    completion_tokens INT DEFAULT 0 COMMENT '输出token数',
    total_tokens INT DEFAULT 0 COMMENT '总token数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_session_id (org_id, session_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (session_id) REFERENCES ai_chat_session(id) ON DELETE CASCADE,
    FOREIGN KEY (model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表（按 org 隔离）';

-- 10) RAG 任务表（按 org 隔离）
CREATE TABLE ai_rag_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
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
    UNIQUE KEY uk_org_task_id (org_id, task_id),
    INDEX idx_org_status_retry (org_id, status, retry_count),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 任务表（按 org 隔离）';

-- 11) MCP 网关实例表（按 org 隔离）
CREATE TABLE mcp_gateway (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '网关唯一标识（业务ID）',
    gateway_name    VARCHAR(100) NOT NULL COMMENT '网关名称',
    gateway_desc    VARCHAR(500)          COMMENT '网关描述',
    gateway_version VARCHAR(20)           COMMENT '网关版本号',
    gateway_instructions TEXT              COMMENT '网关使用说明（供模型参考）',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_gateway_id (org_id, gateway_id),
    INDEX idx_org_status (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 网关实例表（按 org 隔离）';

-- 12) MCP 网关认证表（按 org 隔离）
CREATE TABLE mcp_gateway_auth (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '网关唯一标识',
    api_key         VARCHAR(128) NOT NULL COMMENT 'API Key',
    rate_limit      INT DEFAULT 100       COMMENT '速率限制（次/分钟）',
    expire_time     DATETIME              COMMENT '过期时间，NULL 表示永不过期',
    status          TINYINT(1) DEFAULT 1  COMMENT '状态：1-启用 0-禁用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_gateway_api_key (org_id, gateway_id, api_key),
    INDEX idx_org_api_key (org_id, api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 网关认证表（按 org 隔离）';

-- 13) MCP 工具注册表（按 org 隔离，补充 tool_key/risk_level）
CREATE TABLE mcp_tool_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
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
    UNIQUE KEY uk_org_gateway_tool (org_id, gateway_id, tool_name),
    UNIQUE KEY uk_org_tool_key (org_id, tool_key),
    INDEX idx_org_gateway_id (org_id, gateway_id),
    INDEX idx_org_status (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具注册表（按 org 隔离）';

-- 14) MCP 工具参数映射表（按 org 隔离）
CREATE TABLE mcp_tool_mapping (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
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
    UNIQUE KEY uk_org_mapping_node (org_id, tool_id, mapping_type, parent_id, field_name),
    INDEX idx_org_tool_id (org_id, tool_id),
    INDEX idx_org_parent_id (org_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具参数映射表（按 org 隔离）';

-- 15) MCP 工具 Schema 缓存表（按 org 隔离）
CREATE TABLE mcp_tool_schema (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '所属工具ID',
    schema_version  INT DEFAULT 1         COMMENT 'Schema 版本号',
    input_schema    TEXT                  COMMENT '输入 Schema（JSON）',
    output_schema   TEXT                  COMMENT '输出 Schema（JSON）',
    schema_hash     VARCHAR(64)           COMMENT 'Schema 内容的 SHA-256 哈希',
    is_active       TINYINT(1) DEFAULT 1  COMMENT '是否为当前活跃版本',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_tool_schema_version (org_id, gateway_id, tool_id, schema_version),
    INDEX idx_org_tool_active (org_id, tool_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具 Schema 缓存表（按 org 隔离）';

-- 16) MCP 工具绑定关系表（按 org 隔离）
CREATE TABLE mcp_tool_binding (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '组织ID',
    gateway_id      VARCHAR(64)  NOT NULL COMMENT '所属网关ID',
    tool_id         BIGINT       NOT NULL COMMENT '工具ID',
    bind_type       VARCHAR(20)  NOT NULL COMMENT '绑定类型：MODEL/SESSION/AGENT_VERSION',
    bind_target_id  BIGINT       NOT NULL COMMENT '绑定目标ID',
    enabled         TINYINT(1) DEFAULT 1  COMMENT '是否启用',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_binding (org_id, tool_id, bind_type, bind_target_id),
    INDEX idx_org_bind_target (org_id, bind_type, bind_target_id),
    INDEX idx_org_gateway_id (org_id, gateway_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 工具绑定关系表（按 org 隔离）';

-- 17) 工具风险策略（按 org + toolKey）
CREATE TABLE tool_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    tool_key VARCHAR(200) NOT NULL COMMENT '工具key',
    risk_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级：LOW/MEDIUM/HIGH',
    approval_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要审批（1是0否）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_tool (org_id, tool_key),
    INDEX idx_org_enabled (org_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具风险策略（按 org + toolKey）';

-- =====================================================
-- C. 多 Agent 平台（控制面 + 运行面 + 审批）
-- =====================================================

-- 1) Agent
CREATE TABLE agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    agent_code VARCHAR(64) NOT NULL COMMENT 'Agent 对外唯一编码',
    agent_name VARCHAR(100) NOT NULL COMMENT 'Agent 名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    current_published_version_id BIGINT DEFAULT NULL COMMENT '当前发布版本ID',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_agent_code (org_id, agent_code),
    INDEX idx_org_status (org_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 表（按 org 隔离）';

-- 2) AgentVersion
CREATE TABLE agent_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    version_no INT NOT NULL COMMENT '版本号（递增）',
    state VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT '变更摘要',
    prompt_template_id BIGINT DEFAULT NULL COMMENT '模板ID',
    prompt_template_version_no INT DEFAULT NULL COMMENT '模板版本号（发布固化）',
    template_params_json JSON DEFAULT NULL COMMENT '模板参数（JSON）',
    system_prompt_snapshot MEDIUMTEXT DEFAULT NULL COMMENT '系统提示词快照（发布固化）',
    output_contract_version VARCHAR(20) NOT NULL DEFAULT 'v1' COMMENT '输出契约版本',
    output_contract_options_json JSON DEFAULT NULL COMMENT '输出契约选项（JSON）',
    model_strategy_type VARCHAR(30) NOT NULL DEFAULT 'TASK_TYPE_POLICY' COMMENT '模型策略：TASK_TYPE_POLICY/FIXED_MODEL',
    task_type_code VARCHAR(50) DEFAULT NULL COMMENT '任务类型编码（策略为 TASK_TYPE_POLICY 时）',
    fixed_model_id BIGINT DEFAULT NULL COMMENT '固定模型ID（策略为 FIXED_MODEL 时）',
    rag_mode VARCHAR(20) NOT NULL DEFAULT 'OPTIONAL' COMMENT 'RAG模式：DISABLED/OPTIONAL/REQUIRED',
    default_rag_tags_json JSON DEFAULT NULL COMMENT '默认RAG标签（JSON数组）',
    allowed_rag_tags_json JSON DEFAULT NULL COMMENT '允许覆盖RAG标签（JSON数组）',
    tool_policy_mode VARCHAR(30) NOT NULL DEFAULT 'ALLOWLIST_ONLY' COMMENT '工具策略：ALLOWLIST_ONLY',
    allowed_tool_keys_json JSON DEFAULT NULL COMMENT '允许工具集合（toolKey JSON数组）',
    tool_risk_policy_json JSON DEFAULT NULL COMMENT '工具风险策略（JSON）',
    timeout_ms INT DEFAULT 60000 COMMENT '超时毫秒',
    max_turns INT DEFAULT 20 COMMENT '最大轮次',
    temperature DECIMAL(4,2) DEFAULT 0.70 COMMENT '温度',
    repair_retry_times INT DEFAULT 2 COMMENT '结构化修复重试次数',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_agent_version_no (agent_id, version_no),
    INDEX idx_org_agent_state (org_id, agent_id, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 版本表（按 org 隔离）';

-- 3) PromptTemplate（GLOBAL/ORG）
CREATE TABLE prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    scope VARCHAR(10) NOT NULL DEFAULT 'ORG' COMMENT '作用域：GLOBAL/ORG',
    org_id BIGINT NOT NULL COMMENT '组织ID（GLOBAL 固定为 0）',
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
    UNIQUE KEY uk_org_template_code (org_id, template_code),
    INDEX idx_scope_state (scope, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词模板资产';

-- 4) AgentSchedule（XXL）
CREATE TABLE agent_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    cron VARCHAR(100) NOT NULL COMMENT 'Cron 表达式',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    xxl_job_id BIGINT DEFAULT NULL COMMENT 'XXL Job ID',
    payload_template_json JSON DEFAULT NULL COMMENT '调度入参模板（JSON）',
    created_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_agent_schedule (org_id, agent_id),
    INDEX idx_org_enabled (org_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 调度配置（按 org 隔离）';

-- 5) AgentRun（runId=traceId）
CREATE TABLE agent_run (
    run_id VARCHAR(64) PRIMARY KEY COMMENT '运行ID（建议=traceId）',
    org_id BIGINT NOT NULL COMMENT '组织ID',
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
    prompt_tokens INT DEFAULT 0 COMMENT 'prompt tokens',
    completion_tokens INT DEFAULT 0 COMMENT 'completion tokens',
    total_tokens INT DEFAULT 0 COMMENT 'total tokens',
    tool_call_count INT DEFAULT 0 COMMENT '工具调用次数',
    tool_denied_count INT DEFAULT 0 COMMENT '工具拒绝次数',
    repair_attempts INT DEFAULT 0 COMMENT '结构化修复次数',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时ms',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    INDEX idx_org_time (org_id, started_at),
    INDEX idx_agent_version (agent_id, agent_version_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 运行记录（按 org 隔离）';

-- 6) 审批单（高风险工具默认生成审批单）
CREATE TABLE approval_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    approval_type VARCHAR(30) NOT NULL COMMENT '审批类型：TOOL_INVOKE',
    status VARCHAR(30) NOT NULL COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED',
    run_id VARCHAR(64) NOT NULL COMMENT '关联 runId',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    agent_version_id BIGINT NOT NULL COMMENT 'AgentVersion ID',
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
    INDEX idx_org_status (org_id, status),
    INDEX idx_run (run_id),
    INDEX idx_tool (tool_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审批单（按 org 隔离）';

-- 7) 运行上下文快照（用于审批通过后自动续跑）
CREATE TABLE agent_run_context (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    run_id VARCHAR(64) NOT NULL COMMENT '运行ID',
    status VARCHAR(30) NOT NULL COMMENT '上下文状态：SAVED/RESUMED/EXPIRED',
    snapshot_json JSON NOT NULL COMMENT '可恢复上下文快照（JSON）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_org_run (org_id, run_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行上下文快照（按 org 隔离）';

-- =====================================================
-- D. 初始化数据（默认组织 ROOT）
-- =====================================================

-- 初始化数据：模型配置（示例）
INSERT INTO ai_model_config (org_id, model_name, model_type, api_key, base_url, enabled, tool_enabled, priority)
VALUES
(@root_org_id, 'GPT-4', 'OPENAI', 'sk-placeholder', 'http://127.0.0.1:8045', 1, 1, 90),
(@root_org_id, 'Claude-3.5-Sonnet', 'ANTHROPIC', 'sk-ant-placeholder', 'https://api.anthropic.com', 1, 1, 95),
(@root_org_id, 'Gemini-3-Flash', 'GEMINI', 'sk-placeholder', 'http://127.0.0.1:8045', 1, 1, 85)
ON DUPLICATE KEY UPDATE
model_type = VALUES(model_type),
api_key = VALUES(api_key),
base_url = VALUES(base_url),
enabled = VALUES(enabled),
tool_enabled = VALUES(tool_enabled),
priority = VALUES(priority),
updated_at = VALUES(updated_at);

SELECT id INTO @gpt4_id FROM ai_model_config WHERE org_id = @root_org_id AND model_name = 'GPT-4' LIMIT 1;
SELECT id INTO @claude_id FROM ai_model_config WHERE org_id = @root_org_id AND model_name = 'Claude-3.5-Sonnet' LIMIT 1;
SELECT id INTO @gemini_id FROM ai_model_config WHERE org_id = @root_org_id AND model_name = 'Gemini-3-Flash' LIMIT 1;

-- 初始化数据：模型能力
INSERT INTO ai_model_capability (org_id, model_id, max_input_tokens, max_output_tokens, support_function_calling, support_vision, support_streaming, quality_score)
VALUES
(@root_org_id, @gpt4_id, 128000, 4096, 1, 1, 1, 90),
(@root_org_id, @claude_id, 200000, 4096, 1, 1, 1, 95),
(@root_org_id, @gemini_id, 1000000, 8192, 1, 1, 1, 85)
ON DUPLICATE KEY UPDATE
max_input_tokens = VALUES(max_input_tokens),
max_output_tokens = VALUES(max_output_tokens),
support_function_calling = VALUES(support_function_calling),
support_vision = VALUES(support_vision),
support_streaming = VALUES(support_streaming),
quality_score = VALUES(quality_score),
updated_at = VALUES(updated_at);

-- 初始化数据：任务类型
INSERT INTO ai_task_type (org_id, task_name, task_code, description, preferred_model_id, fallback_model_ids)
VALUES
(@root_org_id, '分析', 'ANALYSIS', '数据分析、逻辑推理等任务', @gemini_id, CONCAT(@claude_id, ',', @gpt4_id)),
(@root_org_id, '写作', 'WRITING', '文章创作、内容生成等任务', @claude_id, CONCAT(@gpt4_id, ',', @gemini_id)),
(@root_org_id, '翻译', 'TRANSLATION', '多语言翻译任务', @gpt4_id, CONCAT(@gemini_id, ',', @claude_id)),
(@root_org_id, '代码生成', 'CODE_GENERATION', '代码编写、调试等任务', @claude_id, CONCAT(@gpt4_id, ',', @gemini_id)),
(@root_org_id, '对话', 'CONVERSATION', '日常对话、问答等任务', @gpt4_id, CONCAT(@gemini_id, ',', @claude_id)),
(@root_org_id, '总结', 'SUMMARIZATION', '文本摘要、总结等任务', @gemini_id, CONCAT(@claude_id, ',', @gpt4_id)),
(@root_org_id, '对接MCP', 'MCP_INTEGRATION', 'MCP协议对接任务', @claude_id, CONCAT(@gpt4_id, ',', @gemini_id))
ON DUPLICATE KEY UPDATE
task_name = VALUES(task_name),
description = VALUES(description),
preferred_model_id = VALUES(preferred_model_id),
fallback_model_ids = VALUES(fallback_model_ids),
updated_at = VALUES(updated_at);

-- 初始化数据：模型激活配置（ROOT org）
INSERT INTO ai_model_activation (org_id, chat_model_id, embedding_model_id)
VALUES
(@root_org_id, @gpt4_id, @gpt4_id)
ON DUPLICATE KEY UPDATE
chat_model_id = VALUES(chat_model_id),
embedding_model_id = VALUES(embedding_model_id),
updated_at = VALUES(updated_at);

SELECT '数据库初始化完成（含 org 隔离 + 多Agent平台核心表）' AS message, NOW() AS executed_at;
