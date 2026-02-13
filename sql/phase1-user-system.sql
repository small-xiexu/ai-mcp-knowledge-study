-- =====================================================
-- Phase 1 用户体系初始化脚本
-- 说明：
-- 1) 创建用户/角色/权限核心表
-- 2) 初始化默认管理员账号与权限
-- 3) 默认租户为 default
-- =====================================================

SET NAMES utf8mb4;
USE ai_model_orchestration;

CREATE TABLE IF NOT EXISTS sys_user (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    username         VARCHAR(64) NOT NULL COMMENT '登录用户名',
    display_name     VARCHAR(128) NOT NULL COMMENT '显示名称',
    email            VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    mobile           VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    password_hash    VARCHAR(255) NOT NULL COMMENT '密码哈希',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用 2锁定',
    is_super_admin   TINYINT NOT NULL DEFAULT 0 COMMENT '是否平台超管：1是 0否',
    last_login_at    DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip    VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_tenant_username (tenant_id, username),
    KEY idx_sys_user_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    role_code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name        VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_scope       VARCHAR(32) NOT NULL DEFAULT 'TENANT' COMMENT '角色范围',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_tenant_code (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    permission_code  VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name  VARCHAR(128) NOT NULL COMMENT '权限名称',
    resource_type    VARCHAR(64) NOT NULL COMMENT '资源类型',
    action           VARCHAR(64) NOT NULL COMMENT '动作',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

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

CREATE TABLE IF NOT EXISTS sys_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    org_code         VARCHAR(64) NOT NULL COMMENT '组织编码',
    org_name         VARCHAR(128) NOT NULL COMMENT '组织名称',
    parent_id        BIGINT DEFAULT NULL COMMENT '父组织ID',
    org_path         VARCHAR(1000) DEFAULT NULL COMMENT '组织路径',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_org_tenant_code (tenant_id, org_code),
    KEY idx_sys_org_tenant_parent (tenant_id, parent_id),
    CONSTRAINT fk_sys_org_parent FOREIGN KEY (parent_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统组织表';

CREATE TABLE IF NOT EXISTS sys_user_org (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    org_id           BIGINT NOT NULL COMMENT '组织ID',
    is_primary       TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织',
    joined_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_sys_user_org (tenant_id, user_id, org_id),
    KEY idx_sys_user_org_org (tenant_id, org_id),
    CONSTRAINT fk_sys_user_org_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_org_org FOREIGN KEY (org_id) REFERENCES sys_org (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组织关系表';

CREATE TABLE IF NOT EXISTS sys_api_key (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
    owner_user_id    BIGINT NOT NULL COMMENT '归属用户ID',
    access_key       VARCHAR(64) NOT NULL COMMENT '访问Key',
    secret_hash      VARCHAR(255) NOT NULL COMMENT '密钥哈希',
    scopes           JSON DEFAULT NULL COMMENT '权限范围',
    status           TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    expire_at        DATETIME DEFAULT NULL COMMENT '过期时间',
    last_used_at     DATETIME DEFAULT NULL COMMENT '最后使用时间',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_api_key_access_key (access_key),
    KEY idx_sys_api_key_owner (tenant_id, owner_user_id),
    CONSTRAINT fk_sys_api_key_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥表';

CREATE TABLE IF NOT EXISTS sys_audit_event (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    tenant_id        VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '租户ID',
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
    KEY idx_sys_audit_operator_time (tenant_id, operator_id, occurred_at),
    KEY idx_sys_audit_resource (tenant_id, resource_type, resource_id),
    KEY idx_sys_audit_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一审计事件表';

SET @tenant_id = 'default';
SET @now = NOW();

INSERT INTO sys_permission (permission_code, permission_name, resource_type, action, status, created_at, updated_at)
VALUES
('user:read', '读取用户', 'user', 'read', 1, @now, @now),
('user:write', '编辑用户', 'user', 'write', 1, @now, @now),
('role:read', '读取角色', 'role', 'read', 1, @now, @now),
('role:write', '编辑角色', 'role', 'write', 1, @now, @now),
('audit:read', '读取审计', 'audit', 'read', 1, @now, @now)
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
resource_type = VALUES(resource_type),
action = VALUES(action),
status = VALUES(status),
updated_at = VALUES(updated_at);

INSERT INTO sys_role (tenant_id, role_code, role_name, role_scope, status, remark, created_at, updated_at)
VALUES
(@tenant_id, 'PLATFORM_ADMIN', '平台管理员', 'PLATFORM', 1, '平台级全权限', @now, @now)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
role_scope = VALUES(role_scope),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

INSERT INTO sys_user (
    tenant_id, username, display_name, email, password_hash,
    status, is_super_admin, created_at, updated_at
)
VALUES (
    @tenant_id, 'admin', '平台管理员', 'admin@example.com',
    'CHANGE_ME_BCRYPT_HASH',
    1, 1, @now, @now
)
ON DUPLICATE KEY UPDATE
display_name = VALUES(display_name),
email = VALUES(email),
status = VALUES(status),
is_super_admin = VALUES(is_super_admin),
updated_at = VALUES(updated_at);

SELECT id INTO @admin_user_id FROM sys_user WHERE tenant_id = @tenant_id AND username = 'admin' LIMIT 1;
SELECT id INTO @platform_admin_role_id FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'PLATFORM_ADMIN' LIMIT 1;

INSERT INTO sys_org (tenant_id, org_code, org_name, parent_id, org_path, status, remark, created_at, updated_at)
VALUES
(@tenant_id, 'ROOT', '默认组织', NULL, '/ROOT', 1, '默认根组织', @now, @now)
ON DUPLICATE KEY UPDATE
org_name = VALUES(org_name),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

SELECT id INTO @root_org_id FROM sys_org WHERE tenant_id = @tenant_id AND org_code = 'ROOT' LIMIT 1;

INSERT INTO sys_user_role (tenant_id, user_id, role_id, granted_by, granted_at)
VALUES
(@tenant_id, @admin_user_id, @platform_admin_role_id, @admin_user_id, @now)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

INSERT INTO sys_user_org (tenant_id, user_id, org_id, is_primary, joined_at)
VALUES
(@tenant_id, @admin_user_id, @root_org_id, 1, @now)
ON DUPLICATE KEY UPDATE
is_primary = VALUES(is_primary),
joined_at = VALUES(joined_at);

INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @platform_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

SELECT 'Phase 1 用户体系初始化完成' AS message, NOW() AS executed_at;
