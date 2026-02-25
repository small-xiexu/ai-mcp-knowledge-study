package com.xbk.knowledge.trigger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger API 文档配置
 * 访问地址http://localhost:8080/swagger-ui.html
 *
 * 职责：接口层配置，用于提供文档与切面支持
 * @author sxie
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档信息
     *
     * 统一接口描述入口，便于前端/测试快速发现接口变更。
     * 
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Contact contact = new Contact()
                .name("xiexu")
                .email("xiexu@example.com");
        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");
        Info info = new Info()
                .title("AI 多模型编排系统 API")
                .description("统一管理 OpenAI、Anthropic、Gemini 等多种 AI 模型的编排系统")
                .version("1.0.0")
                .contact(contact)
                .license(license);
        return new OpenAPI()
                .info(info);
    }
}
