-- =====================================================
-- Phase 1 用户体系 MySQL DDL 初稿
-- 项目：ai-mcp-knowledge-study
-- 说明：
-- 1) 对应文档：.codex/Phase1用户体系数据模型草案.md
-- 2) 目标：先单租户运行，结构上为多租户预埋 tenant_id
-- 3) MySQL 版本：8.0+
-- =====================================================

SET NAMES utf8mb4;

-- 如需独立库可取消注释
-- CREATE DATABASE IF NOT EXISTS ai_identity
--   DEFAULT CHARACTER SET utf8mb4
--   DEFAULT COLLATE utf8mb4_unicode_ci;
-- USE ai_identity;

-- =====================================================
-- 1. 用户主表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID（Phase1 默认 default）',
    username         VARCHAR(64) NOT NULL COMMENT '登录用户名',
    display_name     VARCHAR(128) NOT NULL COMMENT '显示名称',
    email            VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    mobile           VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    password_hash    VARCHAR(255) NOT NULL COMMENT '密码哈希',
    password_salt    VARCHAR(64) DEFAULT NULL COMMENT '密码盐（可选）',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用 2锁定',
    is_super_admin   TINYINT NOT NULL DEFAULT 0 COMMENT '是否平台超管：1是 0否',
    last_login_at    DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip    VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_tenant_username (tenant_id, username),
    KEY idx_sys_user_tenant_status (tenant_id, status),
    KEY idx_sys_user_tenant_mobile (tenant_id, mobile),
    KEY idx_sys_user_tenant_email (tenant_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =====================================================
-- 2. 角色表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    role_code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name        VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_scope       VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '角色范围：PLATFORM/TENANT/PROJECT',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by       BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_tenant_code (tenant_id, role_code),
    KEY idx_sys_role_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- =====================================================
-- 3. 权限资源表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    permission_code  VARCHAR(128) NOT NULL COMMENT '权限编码，如 agent:publish',
    permission_name  VARCHAR(128) NOT NULL COMMENT '权限名称',
    resource_type    VARCHAR(64) NOT NULL COMMENT '资源类型，如 agent/tool/workflow/release',
    action           VARCHAR(64) NOT NULL COMMENT '动作，如 read/write/publish/approve',
    http_method      VARCHAR(16) DEFAULT NULL COMMENT 'HTTP 方法（可选）',
    http_path        VARCHAR(255) DEFAULT NULL COMMENT 'HTTP 路径（可选）',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_permission_code (permission_code),
    KEY idx_sys_permission_resource_action (resource_type, action),
    KEY idx_sys_permission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- =====================================================
-- 4. 用户-角色关系表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    role_id          BIGINT NOT NULL COMMENT '角色ID',
    granted_by       BIGINT DEFAULT NULL COMMENT '授权人ID',
    granted_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_sys_user_role (tenant_id, user_id, role_id),
    KEY idx_sys_user_role_role (tenant_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

-- =====================================================
-- 5. 角色-权限关系表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    role_id          BIGINT NOT NULL COMMENT '角色ID',
    permission_id    BIGINT NOT NULL COMMENT '权限ID',
    granted_by       BIGINT DEFAULT NULL COMMENT '授权人ID',
    granted_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_sys_role_permission (tenant_id, role_id, permission_id),
    KEY idx_sys_role_permission_perm (permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系表';

-- =====================================================
-- 6. 组织表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    org_code         VARCHAR(64) NOT NULL COMMENT '组织编码',
    org_name         VARCHAR(128) NOT NULL COMMENT '组织名称',
    parent_id        BIGINT DEFAULT NULL COMMENT '父组织ID',
    org_path         VARCHAR(1000) DEFAULT NULL COMMENT '组织路径（如 /1/3/5）',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_org_tenant_code (tenant_id, org_code),
    KEY idx_sys_org_tenant_parent (tenant_id, parent_id),
    KEY idx_sys_org_tenant_status (tenant_id, status),
    CONSTRAINT fk_sys_org_parent FOREIGN KEY (parent_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统组织表';

-- =====================================================
-- 7. 用户-组织关系表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_user_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    org_id           BIGINT NOT NULL COMMENT '组织ID',
    is_primary       TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织：1是 0否',
    joined_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_sys_user_org (tenant_id, user_id, org_id),
    KEY idx_sys_user_org_org (tenant_id, org_id),
    CONSTRAINT fk_sys_user_org_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_org_org FOREIGN KEY (org_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组织关系表';

-- =====================================================
-- 8. API Key / 服务账号密钥表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_api_key (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    owner_user_id    BIGINT NOT NULL COMMENT '归属用户ID',
    access_key       VARCHAR(64) NOT NULL COMMENT '访问Key（明文标识）',
    secret_hash      VARCHAR(255) NOT NULL COMMENT '密钥哈希',
    scopes           JSON DEFAULT NULL COMMENT '权限范围（JSON数组）',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    expire_at        DATETIME DEFAULT NULL COMMENT '过期时间',
    last_used_at     DATETIME DEFAULT NULL COMMENT '最后使用时间',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_api_key_access_key (access_key),
    KEY idx_sys_api_key_owner (tenant_id, owner_user_id),
    KEY idx_sys_api_key_status_expire (tenant_id, status, expire_at),
    CONSTRAINT fk_sys_api_key_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥表';

-- =====================================================
-- 9. 审计事件表
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_audit_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    operator_id      BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_type    VARCHAR(32) NOT NULL COMMENT '操作主体类型：user/api_key/system',
    event_type       VARCHAR(64) NOT NULL COMMENT '事件类型：authz/publish/config/tool_call',
    resource_type    VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id      VARCHAR(128) NOT NULL COMMENT '资源ID',
    action           VARCHAR(64) NOT NULL COMMENT '动作',
    request_id       VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    source_ip        VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
    user_agent       VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    old_value        JSON DEFAULT NULL COMMENT '旧值快照',
    new_value        JSON DEFAULT NULL COMMENT '新值快照',
    result           TINYINT NOT NULL DEFAULT 1 COMMENT '执行结果：1成功 0失败',
    error_message    VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    cost_ms          BIGINT DEFAULT NULL COMMENT '耗时(ms)',
    ext              JSON DEFAULT NULL COMMENT '扩展信息',
    occurred_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    KEY idx_sys_audit_operator_time (tenant_id, operator_id, occurred_at),
    KEY idx_sys_audit_resource (tenant_id, resource_type, resource_id),
    KEY idx_sys_audit_request_id (request_id),
    KEY idx_sys_audit_event_time (tenant_id, event_type, occurred_at),
    CONSTRAINT fk_sys_audit_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计事件表';

-- =====================================================
-- 10. 初始化建议（可选）
-- =====================================================
-- 建议初始化角色：
-- PLATFORM_ADMIN / TENANT_ADMIN / AGENT_OWNER / AUDITOR / VIEWER
-- 建议初始化权限：
-- agent:read agent:write agent:publish
-- workflow:read workflow:write
-- tool:read tool:write tool:invoke
-- release:approve
-- audit:read

