package com.xbk.knowledge.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 测试环境基础设施配置
 * 确保 infrastructure 模块的 Bean 被正确扫描和注册
 *
 * @author xiexu
 */
@TestConfiguration
@ComponentScan(basePackages = {
        "com.xbk.knowledge.infrastructure.provider",
        "com.xbk.knowledge.infrastructure.repository",
        "com.xbk.knowledge.infrastructure.audit"
})
public class TestInfrastructureConfig {
}
