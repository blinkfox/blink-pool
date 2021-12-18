package com.blinkfox.pool.starter;

import com.blinkfox.pool.BlinkConfig;
import com.blinkfox.pool.BlinkDataSource;
import com.blinkfox.pool.exception.BlinkPoolException;
import com.blinkfox.pool.starter.properties.BlinkPoolConfigProperties;
import com.blinkfox.pool.starter.properties.BlinkSpringDataSourceProperties;
import javax.annotation.Resource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Blink Pool 连接池数据源的自动配置类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(BlinkDataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({BlinkSpringDataSourceProperties.class, BlinkPoolConfigProperties.class})
public class BlinkPoolAutoConfiguration {

    /**
     * {@link BlinkSpringDataSourceProperties} 属性配置类的实例.
     */
    @Resource
    private BlinkSpringDataSourceProperties dataSourceProperties;

    /**
     * {@link BlinkPoolConfigProperties} 属性配置类的实例.
     */
    @Resource
    private BlinkPoolConfigProperties blinkConfigProperties;

    /**
     * 创建 blink-pool 数据源的对象实例.
     *
     * @return blink-pool 数据源对象
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        if (this.dataSourceProperties == null || this.blinkConfigProperties == null) {
            throw new BlinkPoolException("[blink-pool 异常] 未初始化 blink-pool 的配置属性类，请配置 SpringBoot 的数据源.");
        }

        return new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(this.dataSourceProperties.getUrl())
                .setDriverClassName(this.dataSourceProperties.getDriverClassName())
                .setUsername(this.dataSourceProperties.getUsername())
                .setPassword(this.dataSourceProperties.getPassword())
                .setPoolName(this.blinkConfigProperties.getPoolName())
                .setMinIdle(this.blinkConfigProperties.getMinIdle())
                .setMaxPoolSize(this.blinkConfigProperties.getMaxPoolSize())
                .setIdleTimeout(this.blinkConfigProperties.getIdleTimeout())
                .setMaxLifetime(this.blinkConfigProperties.getMaxLifetime())
                .setCheckInterval(this.blinkConfigProperties.getCheckInterval())
                .setCheckTimeout(this.blinkConfigProperties.getCheckTimeout())
                .setCheckSql(this.blinkConfigProperties.getCheckSql())
                .setBorrowTimeout(this.blinkConfigProperties.getBorrowTimeout())
                .setAsyncInitIdleConnections(this.blinkConfigProperties.isAsyncInitIdleConnections()));
    }

}
