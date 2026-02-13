-- =====================================================
-- Phase 1 用户体系初始化数据
-- 说明：
-- 1) 初始化默认租户基础角色、权限、管理员账号
-- 2) 密码哈希请上线前替换
-- =====================================================

SET NAMES utf8mb4;
USE ai_model_orchestration;

SET @tenant_id = 'default';
SET @now = NOW();

-- =====================================================
-- A. 初始化权限
-- =====================================================

INSERT INTO sys_permission (permission_code, permission_name, resource_type, action, status, created_at, updated_at)
VALUES
('agent:read',       '读取Agent',           'agent',     'read',    1, @now, @now),
('agent:write',      '编辑Agent',           'agent',     'write',   1, @now, @now),
('agent:publish',    '发布Agent',           'agent',     'publish', 1, @now, @now),
('workflow:read',    '读取Workflow',        'workflow',  'read',    1, @now, @now),
('workflow:write',   '编辑Workflow',        'workflow',  'write',   1, @now, @now),
('tool:read',        '读取Tool',            'tool',      'read',    1, @now, @now),
('tool:write',       '编辑Tool',            'tool',      'write',   1, @now, @now),
('tool:invoke',      '调用Tool',            'tool',      'invoke',  1, @now, @now),
('release:approve',  '审批发布',            'release',   'approve', 1, @now, @now),
('audit:read',       '读取审计',            'audit',     'read',    1, @now, @now),
('user:read',        '读取用户',            'user',      'read',    1, @now, @now),
('user:write',       '编辑用户',            'user',      'write',   1, @now, @now),
('role:read',        '读取角色',            'role',      'read',    1, @now, @now),
('role:write',       '编辑角色',            'role',      'write',   1, @now, @now),
('tenant:admin',     '租户管理',            'tenant',    'admin',   1, @now, @now)
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
resource_type = VALUES(resource_type),
action = VALUES(action),
status = VALUES(status),
updated_at = VALUES(updated_at);

-- =====================================================
-- B. 初始化角色
-- =====================================================

INSERT INTO sys_role (tenant_id, role_code, role_name, role_scope, status, remark, created_at, updated_at)
VALUES
(@tenant_id, 'PLATFORM_ADMIN', '平台管理员', 'PLATFORM', 1, '平台级全权限', @now, @now),
(@tenant_id, 'TENANT_ADMIN',   '租户管理员', 'TENANT',   1, '租户级管理权限', @now, @now),
(@tenant_id, 'AGENT_OWNER',    'Agent负责人','TENANT',   1, 'Agent配置与发布权限', @now, @now),
(@tenant_id, 'AUDITOR',        '审计员',     'TENANT',   1, '审计只读', @now, @now),
(@tenant_id, 'VIEWER',         '观察者',     'TENANT',   1, '平台只读访问', @now, @now)
ON DUPLICATE KEY UPDATE
role_name = VALUES(role_name),
role_scope = VALUES(role_scope),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

-- =====================================================
-- C. 初始化组织
-- =====================================================

INSERT INTO sys_org (tenant_id, org_code, org_name, parent_id, org_path, status, remark, created_at, updated_at)
VALUES
(@tenant_id, 'ROOT', '默认组织', NULL, '/ROOT', 1, '默认根组织', @now, @now)
ON DUPLICATE KEY UPDATE
org_name = VALUES(org_name),
status = VALUES(status),
remark = VALUES(remark),
updated_at = VALUES(updated_at);

-- =====================================================
-- D. 初始化管理员账号
-- =====================================================
-- 默认密码：请替换为你们实际的 BCrypt 哈希
-- 示例占位：CHANGE_ME_BCRYPT_HASH
INSERT INTO sys_user (
    tenant_id, username, display_name, email, mobile, password_hash,
    status, is_super_admin, created_at, updated_at
)
VALUES (
    @tenant_id, 'admin', '平台管理员', 'admin@example.com', NULL, 'CHANGE_ME_BCRYPT_HASH',
    1, 1, @now, @now
)
ON DUPLICATE KEY UPDATE
display_name = VALUES(display_name),
email = VALUES(email),
status = VALUES(status),
is_super_admin = VALUES(is_super_admin),
updated_at = VALUES(updated_at);

-- 取管理员、角色、组织ID
SELECT id INTO @admin_user_id FROM sys_user WHERE tenant_id = @tenant_id AND username = 'admin' LIMIT 1;
SELECT id INTO @platform_admin_role_id FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'PLATFORM_ADMIN' LIMIT 1;
SELECT id INTO @tenant_admin_role_id   FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'TENANT_ADMIN' LIMIT 1;
SELECT id INTO @agent_owner_role_id    FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'AGENT_OWNER' LIMIT 1;
SELECT id INTO @auditor_role_id        FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'AUDITOR' LIMIT 1;
SELECT id INTO @viewer_role_id         FROM sys_role WHERE tenant_id = @tenant_id AND role_code = 'VIEWER' LIMIT 1;
SELECT id INTO @root_org_id            FROM sys_org  WHERE tenant_id = @tenant_id AND org_code = 'ROOT' LIMIT 1;

-- 绑定管理员角色
INSERT INTO sys_user_role (tenant_id, user_id, role_id, granted_by, granted_at)
VALUES
(@tenant_id, @admin_user_id, @platform_admin_role_id, @admin_user_id, @now),
(@tenant_id, @admin_user_id, @tenant_admin_role_id, @admin_user_id, @now)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 绑定管理员组织
INSERT INTO sys_user_org (tenant_id, user_id, org_id, is_primary, joined_at)
VALUES
(@tenant_id, @admin_user_id, @root_org_id, 1, @now)
ON DUPLICATE KEY UPDATE
is_primary = VALUES(is_primary),
joined_at = VALUES(joined_at);

-- =====================================================
-- E. 角色授权（按最小集合）
-- =====================================================

-- 平台管理员：全权限
INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @platform_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 租户管理员：常用管理权限
INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @tenant_admin_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN (
    'agent:read','agent:write','agent:publish',
    'workflow:read','workflow:write',
    'tool:read','tool:write','tool:invoke',
    'release:approve','audit:read',
    'user:read','user:write','role:read','role:write'
)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- Agent负责人：Agent/Workflow/Tool 发布与调用
INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @agent_owner_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN (
    'agent:read','agent:write','agent:publish',
    'workflow:read','workflow:write',
    'tool:read','tool:invoke',
    'audit:read'
)
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 审计员：审计只读
INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @auditor_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN ('audit:read', 'agent:read', 'workflow:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- 观察者：平台基础只读
INSERT INTO sys_role_permission (tenant_id, role_id, permission_id, granted_by, granted_at)
SELECT @tenant_id, @viewer_role_id, p.id, @admin_user_id, @now
FROM sys_permission p
WHERE p.permission_code IN ('agent:read', 'workflow:read', 'tool:read')
ON DUPLICATE KEY UPDATE
granted_by = VALUES(granted_by),
granted_at = VALUES(granted_at);

-- =====================================================
-- F. 初始化完成标识
-- =====================================================
SELECT 'Phase 1 用户体系初始化完成' AS message, @tenant_id AS tenant_id, @admin_user_id AS admin_user_id, NOW() AS executed_at;
