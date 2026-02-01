package com.xbk.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 职责：应用启动入口，用于统一装配并引导运行
 * @author xiexu
 * @date 2026/1/17 08:27
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Application {

    /**
     * 对外暴露 main 作为调用入口，便于上层复用。
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
