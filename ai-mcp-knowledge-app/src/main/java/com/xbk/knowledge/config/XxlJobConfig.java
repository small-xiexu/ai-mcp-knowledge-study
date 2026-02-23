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

    @Value("${xxl.job.executor.admin-addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    /**
     * 构建并注册 XXL-Job 执行器。
     *
     * @return 返回 XxlJobSpringExecutor 数据。
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
