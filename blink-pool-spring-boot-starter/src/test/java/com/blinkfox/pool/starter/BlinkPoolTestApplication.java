package com.blinkfox.pool.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用于测试在 SpringBoot 中使用该 blink-pool 的 Spring Boot Starter 的测试类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@SpringBootApplication
class BlinkPoolTestApplication {

    /**
     * SpringBoot 应用程序主入口.
     *
     * @param args 数组参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BlinkPoolTestApplication.class, args);
    }

}
