-- =====================================================
-- AI 多模型编排系统 - 数据库初始化脚本
-- =====================================================
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- 作者：xiexu
-- 日期：2026-01-27
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_model_orchestration
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_model_orchestration;

-- =====================================================
-- 1. 模型配置表
-- =====================================================
CREATE TABLE ai_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(50) NOT NULL COMMENT '模型类型(OPENAI/ANTHROPIC/GEMINI)',
    api_key VARCHAR(500) NOT NULL COMMENT 'API密钥',
    base_url VARCHAR(500) NOT NULL COMMENT 'API地址',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用(0:禁用 1:启用)',
    priority INT DEFAULT 0 COMMENT '优先级(数值越大越优先；用于默认/扩展策略排序，是否生效取决于策略实现)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_model_type (model_type),
    INDEX idx_enabled (enabled),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- =====================================================
-- 2. 模型能力表
-- =====================================================
CREATE TABLE ai_model_capability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
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
    UNIQUE KEY uk_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型能力表';

-- =====================================================
-- 3. 任务类型表
-- =====================================================
CREATE TABLE ai_task_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编码',
    description TEXT COMMENT '任务描述',
    preferred_model_id BIGINT COMMENT '首选模型ID',
    fallback_model_ids VARCHAR(500) COMMENT '备用模型ID列表(逗号分隔)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_code (task_code),
    FOREIGN KEY (preferred_model_id) REFERENCES ai_model_config(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务类型表';

-- =====================================================
-- 4. 调用日志表
-- =====================================================
CREATE TABLE ai_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    task_type VARCHAR(50) COMMENT '任务类型',
    request_content TEXT COMMENT '请求内容',
    response_content TEXT COMMENT '响应内容',
    tokens_used INT DEFAULT 0 COMMENT '使用token数',
    response_time BIGINT DEFAULT 0 COMMENT '响应时间(ms)',
    status VARCHAR(20) NOT NULL COMMENT '状态(SUCCESS/FAILED/FALLBACK)',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_model_id (model_id),
    INDEX idx_task_type (task_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (model_id) REFERENCES ai_model_config(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

-- =====================================================
-- 5. 配置审计表
-- =====================================================
CREATE TABLE ai_config_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    record_id BIGINT NOT NULL COMMENT '记录ID',
    operation VARCHAR(20) NOT NULL COMMENT '操作(INSERT/UPDATE/DELETE)',
    old_value TEXT COMMENT '旧值(JSON)',
    new_value TEXT COMMENT '新值(JSON)',
    operator VARCHAR(100) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_table_name (table_name),
    INDEX idx_record_id (record_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计日志表';

-- =====================================================
-- 初始化数据：模型配置
-- =====================================================
INSERT INTO ai_model_config (model_name, model_type, api_key, base_url, enabled, priority) VALUES
('GPT-4', 'OPENAI', 'sk-1256419209eb47ccbabaa98abccfe4c8', 'http://127.0.0.1:8045', 1, 90),
('Claude-3.5-Sonnet', 'ANTHROPIC', 'sk-ant-api-key-placeholder', 'https://api.anthropic.com', 1, 95),
('Gemini-3-Flash', 'GEMINI', 'sk-1256419209eb47ccbabaa98abccfe4c8', 'http://127.0.0.1:8045', 1, 85);

-- =====================================================
-- 初始化数据：模型能力
-- =====================================================
INSERT INTO ai_model_capability (model_id, max_input_tokens, max_output_tokens, support_function_calling, support_vision, support_streaming, quality_score) VALUES
(1, 128000, 4096, 1, 1, 1, 90),  -- GPT-4
(2, 200000, 4096, 1, 1, 1, 95),  -- Claude-3.5-Sonnet
(3, 1000000, 8192, 1, 1, 1, 85); -- Gemini-3-Flash

-- =====================================================
-- 初始化数据：任务类型
-- =====================================================
INSERT INTO ai_task_type (task_name, task_code, description, preferred_model_id, fallback_model_ids) VALUES
('分析', 'ANALYSIS', '数据分析、逻辑推理等任务', 3, '2,1'),
('写作', 'WRITING', '文章创作、内容生成等任务', 2, '1,3'),
('翻译', 'TRANSLATION', '多语言翻译任务', 1, '3,2'),
('代码生成', 'CODE_GENERATION', '代码编写、调试等任务', 2, '1,3'),
('对话', 'CONVERSATION', '日常对话、问答等任务', 1, '3,2'),
('总结', 'SUMMARIZATION', '文本摘要、总结等任务', 3, '2,1'),
('对接MCP', 'MCP_INTEGRATION', 'MCP协议对接任务', 2, '1,3');

-- =====================================================
-- 完成
-- =====================================================
SELECT '数据库初始化完成！' AS message;
