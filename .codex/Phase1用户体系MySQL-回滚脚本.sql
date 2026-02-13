-- =====================================================
-- Phase 1 用户体系 MySQL 回滚脚本
-- 说明：
-- 1) 回滚用户中心新增表
-- 2) 回滚现有业务表预埋字段（tenant/operator/request）
-- 3) 会影响数据，请先备份
-- =====================================================

SET NAMES utf8mb4;
USE ai_model_orchestration;

-- =====================================================
-- A. 回滚现有业务表预埋字段
-- =====================================================

ALTER TABLE ai_call_log      DROP COLUMN IF EXISTS request_id;
ALTER TABLE ai_call_log      DROP COLUMN IF EXISTS operator_id;
ALTER TABLE ai_call_log      DROP COLUMN IF EXISTS tenant_id;

ALTER TABLE ai_config_audit  DROP COLUMN IF EXISTS request_id;
ALTER TABLE ai_config_audit  DROP COLUMN IF EXISTS operator_id;
ALTER TABLE ai_config_audit  DROP COLUMN IF EXISTS tenant_id;

ALTER TABLE ai_model_config      DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_task_type         DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_mcp_server_config DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_model_activation  DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_chat_session      DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_chat_message      DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ai_rag_task          DROP COLUMN IF EXISTS tenant_id;

ALTER TABLE mcp_gateway       DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE mcp_gateway_auth  DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE mcp_tool_registry DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE mcp_tool_mapping  DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE mcp_tool_schema   DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE mcp_tool_binding  DROP COLUMN IF EXISTS tenant_id;

-- =====================================================
-- B. 删除用户中心表（按依赖逆序）
-- =====================================================

DROP TABLE IF EXISTS sys_audit_event;
DROP TABLE IF EXISTS sys_api_key;
DROP TABLE IF EXISTS sys_user_org;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_org;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

-- =====================================================
-- C. 回滚完成标识
-- =====================================================
SELECT 'Phase 1 用户体系回滚完成' AS message, NOW() AS executed_at;

