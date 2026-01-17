package com.xbk.knowledge;

import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author xiexu
 * @date 2026/1/17 08:27
 */
@SpringBootApplication(exclude = {OllamaAutoConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
