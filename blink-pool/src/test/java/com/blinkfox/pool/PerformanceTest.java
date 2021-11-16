package com.blinkfox.pool;

import com.alibaba.druid.pool.DruidDataSource;
import com.blinkfox.stalker.Stalker;
import com.blinkfox.stalker.config.Options;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.vibur.dbcp.ViburDBCPDataSource;

/**
 * 这是用来测试各个主流的连接池性能的数据源测试类.
 *
 * @author blinkfox on 2021-10-20.
 * @since 1.0.0
 */
@Slf4j
@Disabled("仅用于手动性能测试.")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceTest {

    /**
     * 总请求数.
     */
    private static final int TOTAL_REQUEST = 100000;

    /**
     * 绝对并发数.
     */
    private static final int CONCURRENT = 100;

    /**
     * 预热次数.
     */
    private static final int WARMUPS = 10000;

    /**
     * 最小闲置连接数.
     */
    private static final int MIN_IDLE = 10;

    /**
     * 最大连接数.
     */
    private static final int MAX_SIZE = 20;

    /**
     * 测试本 blink-pool 库连接池性能的方法.
     */
    @Test
    @Order(1)
    void testBlinkDataSource() {
        log.info("开始执行 blink-pool 的性能测试 ...");
        BlinkDataSource dataSource = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(PostgresDataSourceTest.JDBC_URL)
                .setUsername(PostgresDataSourceTest.USERNAME)
                .setPassword(PostgresDataSourceTest.PASSWORD)
                .setMinIdle(MIN_IDLE)
                .setMaxPoolSize(MAX_SIZE));
        Assertions.assertFalse(dataSource.isClosed());

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("blink-pool").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 blink-pool 的性能测试完毕.\n");

        log.info("获取连接的平均时间: {} ms", dataSource.getBorrowsSumMillis() / dataSource.getTotalBorrows());
        log.info("使用连接的平均时间: {} ms", dataSource.getUsedSumMillis() / dataSource.getTotalBorrows());

        // 关闭数据源.
        dataSource.close();
    }

    /**
     * 测试 HikariCP 连接池性能的方法.
     */
    @Test
    @Order(2)
    void testHikariDataSource() {
        log.info("开始执行 HikariCP 的性能测试 ...");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(PostgresDataSourceTest.JDBC_URL);
        config.setDriverClassName(PostgresDataSourceTest.DRIVER);
        config.setUsername(PostgresDataSourceTest.USERNAME);
        config.setPassword(PostgresDataSourceTest.PASSWORD);
        config.setMinimumIdle(MIN_IDLE);
        config.setMaximumPoolSize(MAX_SIZE);
        HikariDataSource dataSource = new HikariDataSource(config);

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("HikariCP").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接，实际并不会调用此代码.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 HikariCP 的性能测试完毕.\n");

        // 关闭数据源.
        dataSource.close();
    }

    /**
     * 测试 Commons DBCP2 连接池性能的方法.
     */
    @Test
    @Order(3)
    void testCommonsDbcp() throws SQLException {
        log.info("开始执行 commons-dbcp2 的性能测试 ...");
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(PostgresDataSourceTest.JDBC_URL);
        dataSource.setDriverClassName(PostgresDataSourceTest.DRIVER);
        dataSource.setUsername(PostgresDataSourceTest.USERNAME);
        dataSource.setPassword(PostgresDataSourceTest.PASSWORD);
        dataSource.setMinIdle(MIN_IDLE);
        dataSource.setMaxTotal(MAX_SIZE);

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("commons-dbcp2").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 commons-dbcp2 的性能测试完毕.\n");

        // 关闭数据源.
        dataSource.close();
    }

    /**
     * 测试 c3p0 库连接池性能的方法.
     */
    @Test
    @Order(4)
    void testC3p0() throws PropertyVetoException {
        log.info("开始执行 c3p0 的性能测试 ...");
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(PostgresDataSourceTest.JDBC_URL);
        dataSource.setDriverClass(PostgresDataSourceTest.DRIVER);
        dataSource.setUser(PostgresDataSourceTest.USERNAME);
        dataSource.setPassword(PostgresDataSourceTest.PASSWORD);
        dataSource.setInitialPoolSize(MIN_IDLE);
        dataSource.setMinPoolSize(MIN_IDLE);
        dataSource.setMaxPoolSize(MAX_SIZE);

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("c3p0").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 c3p0 的性能测试完毕.\n");

        // 关闭数据源.
        dataSource.close();
    }

    /**
     * 测试 Druid 库连接池性能的方法.
     */
    @Test
    @Order(5)
    void testDruid() {
        log.info("开始执行 Druid 的性能测试 ...");
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(PostgresDataSourceTest.JDBC_URL);
        dataSource.setDriverClassName(PostgresDataSourceTest.DRIVER);
        dataSource.setUsername(PostgresDataSourceTest.USERNAME);
        dataSource.setPassword(PostgresDataSourceTest.PASSWORD);
        dataSource.setMinIdle(MIN_IDLE);
        dataSource.setMaxActive(MAX_SIZE);

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("Druid").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 Druid 的性能测试完毕.\n");

        // 关闭数据源.
        dataSource.close();
    }

    /**
     * 测试 Vibur 库连接池性能的方法.
     */
    @Test
    @Order(6)
    void testVibur() {
        log.info("开始执行 Vibur 的性能测试 ...");
        ViburDBCPDataSource dataSource = new ViburDBCPDataSource();
        dataSource.setJdbcUrl(PostgresDataSourceTest.JDBC_URL);
        dataSource.setDriverClassName(PostgresDataSourceTest.DRIVER);
        dataSource.setUsername(PostgresDataSourceTest.USERNAME);
        dataSource.setPassword(PostgresDataSourceTest.PASSWORD);
        dataSource.setPoolInitialSize(MIN_IDLE);
        dataSource.setPoolMaxSize(MAX_SIZE);
        dataSource.start();

        // 模拟发起并发请求
        Stalker.run(Options.of(TOTAL_REQUEST, CONCURRENT).named("Vibur").warmups(WARMUPS), () -> {
            try (Connection connection = dataSource.getConnection()) {
                // 假装使用一下该连接.
                if (log.isDebugEnabled()) {
                    log.debug("connection: {}.", connection);
                }
            } catch (SQLException e) {
                log.error("执行 SQL 失败.", e);
            }
        });
        log.info("执行 Vibur 的性能测试完毕.\n");

        // 关闭数据源.
        dataSource.close();
    }

}
