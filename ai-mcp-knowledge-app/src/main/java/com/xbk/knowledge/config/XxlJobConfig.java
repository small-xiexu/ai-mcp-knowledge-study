package com.xbk.knowledge.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器配置
 *
 * 职责：配置 XXL-Job 执行器，注册到调度中心
 * @author sxie
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    /**
     * XXL-Admin 地址列表。
     */
    @Value("${xxl.job.executor.admin-addresses}")
    private String adminAddresses;

    /**
     * 执行器应用名。
     */
    @Value("${xxl.job.executor.appname}")
    private String appname;

    /**
     * 执行器端口。
     */
    @Value("${xxl.job.executor.port}")
    private int port;

    /**
     * 执行器日志路径。
     */
    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    /**
     * 日志保留天数。
     */
    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    /**
     * XXL-Admin 访问令牌。
     */
    @Value("${xxl.job.accessToken}")
    private String accessToken;

    /**
     * 构建并注册 XXL-Job 执行器。
     * 
     * @return XxlJobSpringExecutor 数据。
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("初始化 XXL-Job 执行器");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        return xxlJobSpringExecutor;
    }
}
