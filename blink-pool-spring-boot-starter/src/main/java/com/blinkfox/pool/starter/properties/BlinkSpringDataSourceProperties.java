package com.blinkfox.pool.starter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring 单数据源 DataSource 的属性配置类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties("spring.datasource")
public class BlinkSpringDataSourceProperties {

    /**
     * JDBC 连接的 URL.
     */
    private String url;

    /**
     * JDBC 连接的驱动类名称.
     */
    private String driverClassName;

    /**
     * JDBC 连接的用户名.
     */
    private String username;

    /**
     * JDBC 连接的密码.
     */
    private String password;

}
