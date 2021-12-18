package com.blinkfox.pool;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 用于测试 Sybase 使用连接池的情况.
 *
 * @author blinkfox on 2021-10-30.
 * @since v1.0.0
 */
@Slf4j
@Disabled("仅用于手动测试执行.")
class SybaseDataSourceTest {

    /**
     * JDBC 连接池.
     */
    private static final String JDBC_URL = "jdbc:jtds:sybase://127.0.0.1:5000/TEST;charset=UTF-8";

    /**
     * 驱动.
     */
    private static final String DRIVER = "net.sourceforge.jtds.jdbc.Driver";

    /**
     * 用户名.
     */
    private static final String USERNAME = "user";

    /**
     * 密码.
     */
    private static final String PASSWORD = "password";

    private static BlinkDataSource dataSource;

    @BeforeAll
    static void init() {
        long startTime = System.currentTimeMillis();
        dataSource = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(JDBC_URL)
                .setDriverClassName(DRIVER)
                .setUsername(USERNAME)
                .setPassword(PASSWORD)
                .setCheckInterval(30)
                .setCheckSql(BlinkConfig.SIMPLE_CHECK_SQL)
                .setAsyncInitIdleConnections(false));
        log.info("创建 Sybase 数据库连接池完成，耗时:【{} ms】", System.currentTimeMillis() - startTime);
    }

    @Test
    void testSybase() throws SQLException {
        QueryRunner queryRunner = new QueryRunner(dataSource);
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> results = queryRunner.query("select 1", new MapListHandler());
        log.info("执行耗时: {} ms", System.currentTimeMillis() - startTime);
        Assertions.assertTrue(results.size() > 0);
    }

}
