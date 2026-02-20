package com.xbk.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * XXL-Job Admin 对接配置
 * 统一管理登录与执行器参数
 *
 * @author sxie
 */
@Data
@Component
@ConfigurationProperties(prefix = "xxl.admin")
public class XxlAdminProperties {

    /**
     * XXL-Job Admin 基础地址
     */
    private String baseUrl;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 执行器 AppName
     */
    private String appName;

    /**
     * Cookie 缓存秒数
     */
    private Integer cookieTtlSeconds = 1800;

    /**
     * 任务下拉缓存秒数
     */
    private Integer jobCacheTtlSeconds = 600;
}
