-- ============================================================
-- Gateway 融合回滚脚本
-- 说明：回滚 gateway 融合新增的表结构
-- 注意：会删除相关业务数据，请先备份
-- ============================================================

DROP TABLE IF EXISTS mcp_tool_binding;
DROP TABLE IF EXISTS mcp_tool_schema;
DROP TABLE IF EXISTS mcp_tool_mapping;
DROP TABLE IF EXISTS mcp_tool_registry;
DROP TABLE IF EXISTS mcp_gateway_auth;
DROP TABLE IF EXISTS mcp_gateway;
