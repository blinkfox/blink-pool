package com.blinkfox.pool.starter.properties;

import com.blinkfox.pool.BlinkConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Blink Pool 关键连接池配置的属性类.
 *
 * @author blinkfox on 2021-11-17.
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties("spring.datasource.blink-pool")
public class BlinkPoolConfigProperties {

    /**
     * 连接池的名称.
     */
    private String poolName = BlinkConfig.DEFAULT_POOL_NAME;

    /**
     * 连接池最小空闲连接数.
     */
    private int minIdle = BlinkConfig.DEFAULT_MIN_POOL_SIZE;

    /**
     * 连接池最大连接数.
     */
    private int maxPoolSize = BlinkConfig.DEFAULT_MAX_POOL_SIZE;

    /**
     * 连接池中多余连接的默认闲置时间，单位秒（s）.
     */
    private int idleTimeout = BlinkConfig.DEFAULT_IDLE_TIMEOUT;

    /**
     * 连接的最长存活时间，单位毫秒 (ms).
     *
     * <p>默认 30 分钟，最少 1 分钟.</p>
     */
    private long maxLifetime = BlinkConfig.DEFAULT_LIFE_TIME;

    /**
     * 从连接池中借用连接的超时时间，单位毫秒 (ms)，默认为 30 秒钟.
     *
     * <p>如果正在被借用（使用）中的连接数大于等于了最大连接数，此时再从数据库连接池中借用连接时会阻塞，
     * 直到从连接池中获取到连接为止，如果在本超时时间之内还没有获取到连接，那么就直接抛出异常.</p>
     */
    private long borrowTimeout = BlinkConfig.DEFAULT_BORROW_TIMEOUT;

    /**
     * 检查数据库连接是否有效的最少间隔时间，单位毫秒（ms），默认最少是间隔 2 秒钟，即: 2000 ms.
     *
     * <p>从连接池中获取连接时会检查该数据库连接是否仍然有效，
     * 由于每次检查时耗时较长，且大多数情况下都是有效的，为了优化性能，会隔一段时间才进行检查.</p>
     *
     * <ul>
     *     <ol>如果该间隔值小于 0，则表示不做连接的有效性检查，除非特殊情况，通常不建议这样设置.</ol>
     *     <ol>如果该间隔值等于 0，则表示始终都做连接的有效性检查，这样每次从连接池中获取连接时都会进行检查，
     *         对性能有较大的影响，仅适合于对连接可靠性要求高的场景.</ol>
     *     <ol>如果该间隔值大于 0，则表示至少经过了该间隔大小的时间，才会对连接进行有效性检查，默认至少 2 秒钟才检查一次.</ol>
     * </ul>
     */
    private long checkInterval = BlinkConfig.DEFAULT_CHECK_INTERVAL;

    /**
     * 检查数据库连接是否有效时的超时时间，单位秒 (s), 默认是 5 秒钟，最少可配置 1 秒钟.
     */
    private int checkTimeout = BlinkConfig.DEFAULT_CHECK_TIMEOUT;

    /**
     * 用于设置检查连接是否有效的 SQL.
     *
     * <p>通常对于支持 JDBC4 的驱动不要设置此 SQL 值，除非你的 JDBC 驱动不是 JDBC4 或者不支持 {@code isValid()} 方法。
     * 通常，检查连接是否有效的 SQL 应该极为精简快速的才行，如：{@link BlinkConfig#SIMPLE_CHECK_SQL}。
     * 由于该检查 SQL 效率相比于驱动自身的 {@code isValid()} 方法较低，建议你再把 {@link #checkInterval} 的间隔值设置大一点.</p>
     */
    private String checkSql;

}
