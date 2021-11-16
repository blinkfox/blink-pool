package com.blinkfox.pool;

import com.blinkfox.stalker.Stalker;
import com.blinkfox.stalker.config.Options;
import com.blinkfox.pool.exception.BlinkPoolException;
import com.blinkfox.pool.stat.PoolStatistics;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 这是 PostgreSQL 数据库的数据源测试类.
 *
 * @author blinkfox on 2021-10-20.
 * @since 1.0.0
 */
@Slf4j
@Disabled("这个用于人工手动执行")
class PostgresDataSourceTest {

    /**
     * JDBC 连接池.
     */
    static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:5432/postgres";

    /**
     * 驱动.
     */
    static final String DRIVER = "org.postgresql.Driver";

    /**
     * 用户名.
     */
    static final String USERNAME = "sa";

    /**
     * 密码.
     */
    static final String PASSWORD = "123456";

    private static final int MIN_IDLE = 10;

    private static final String SIMPLE_SQL =
            "select client_addr as client, count(client_addr) as count from pg_stat_activity group by client_addr";

    private static BlinkDataSource blinkDataSource;

    @BeforeAll
    static void init() {
        long startTime = System.currentTimeMillis();
        blinkDataSource = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(JDBC_URL)
                .setDriverClassName(DRIVER)
                .setUsername(USERNAME)
                .setPassword(PASSWORD)
                .setMinIdle(MIN_IDLE));
        log.info("创建数据库连接池完成，耗时:【{} ms】", System.currentTimeMillis() - startTime);
        Assertions.assertEquals(MIN_IDLE, blinkDataSource.getCurrentPoolSize());
        Assertions.assertEquals(BlinkConfig.DEFAULT_MAX_POOL_SIZE, blinkDataSource.getConfig().getMaxPoolSize());
        Assertions.assertEquals(MIN_IDLE, blinkDataSource.getTotalCreations());
        Assertions.assertEquals(0, blinkDataSource.getTotalRealCloseds());
        Assertions.assertEquals(0, blinkDataSource.getTotalBorrows());
        Assertions.assertEquals(0, blinkDataSource.getTotalReturns());
        Assertions.assertEquals(0, blinkDataSource.getTotalInvalids());
    }

    @Test
    void select() throws SQLException {
        QueryRunner queryRunner = new QueryRunner();
        try (Connection connection = blinkDataSource.getConnection()) {
            List<Map<String, Object>> results = queryRunner.query(connection, SIMPLE_SQL, new MapListHandler());
            Assertions.assertTrue(results.size() > 0);
        }
    }

    @Test
    void select2() throws SQLException {
        QueryRunner queryRunner = new QueryRunner(blinkDataSource);
        List<Map<String, Object>> results = queryRunner.query(SIMPLE_SQL, new MapListHandler());
        Assertions.assertTrue(results.size() > 0);
    }

    /**
     * 并发测试.
     */
    @Test
    void testConcurrentConnections() {
        QueryRunner queryRunner = new QueryRunner(blinkDataSource);
        Stalker.run(Options.of(100, 5).warmups(1), () -> {
            List<Map<String, Object>> results;
            try {
                results = queryRunner.query(SIMPLE_SQL, new MapListHandler());
            } catch (SQLException e) {
                throw new BlinkPoolException("测试查询失败了！", e);
            }
            Assertions.assertTrue(results.size() > 0);
        });

        // 打印出连接池的监控统计信息.
        blinkDataSource.printStats();
        Assertions.assertTrue(blinkDataSource.getCurrentPoolSize() >= MIN_IDLE);
        log.info("并发执行 SQL 完毕.");
    }

    @Test
    void creatDatabase() throws SQLException {
        int minIdle = 5;
        int maxSize = 20;
        BlinkDataSource database = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(JDBC_URL)
                .setDriverClassName(DRIVER)
                .setUsername(USERNAME)
                .setPassword(PASSWORD)
                .setMinIdle(minIdle)
                .setMaxPoolSize(maxSize));
        Assertions.assertEquals(minIdle, database.getConfig().getMinIdle());
        Assertions.assertEquals(maxSize, database.getConfig().getMaxPoolSize());
        Assertions.assertEquals(minIdle, database.getCurrentPoolSize());

        PoolStatistics stats = database.getStats();
        Assertions.assertEquals(minIdle, stats.getCreations().longValue());
        Assertions.assertEquals(0, stats.getBorrows().longValue());
        Assertions.assertEquals(0, stats.getReturns().longValue());

        // 测试连接的使用情况.
        Connection connection = database.getConnection();
        Assertions.assertEquals(minIdle - 1, database.getCurrentPoolSize());
        Assertions.assertEquals(1, stats.getBorrows().longValue());
        Assertions.assertEquals(1, database.getCurrentBorrowings());

        // 归还连接.
        Assertions.assertInstanceOf(BlinkConnection.class, connection);
        connection.close();
        Assertions.assertEquals(1, stats.getReturns().longValue());
        Assertions.assertEquals(minIdle, database.getCurrentPoolSize());

        // 关闭数据源.
        database.close();
        Assertions.assertEquals(0, stats.getCreations().longValue());
        Assertions.assertEquals(0, stats.getBorrows().longValue());
        Assertions.assertEquals(0, stats.getReturns().longValue());
    }

    @AfterAll
    static void destroy() {
        if (blinkDataSource != null) {
            Assertions.assertFalse(blinkDataSource.isClosed());
            blinkDataSource.close();
            Assertions.assertTrue(blinkDataSource.isClosed());
            Assertions.assertEquals(0, blinkDataSource.getCurrentPoolSize());
            Assertions.assertTrue(blinkDataSource.getPool().getConnectionQueue().isEmpty());
        }
    }

}
