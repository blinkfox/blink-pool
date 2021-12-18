package com.blinkfox.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.hsqldb.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 数据测试类.
 *
 * @author blinkfox on 2021-10-20.
 * @since 1.0.0
 */
@Slf4j
class HsqldbDataSourceTest {

    private static final String DB_NAME = "db_test";

    private static final int PORT = 23017;

    private static final String JDBC_URL = "jdbc:hsqldb:hsql://localhost:" + PORT + "/" + DB_NAME;

    private static final String DRIVER = "org.hsqldb.jdbc.JDBCDriver";

    private static BlinkDataSource blinkDataSource;

    @BeforeAll
    static void init() {
        // 初始化 HSQLDB 的数据库实例.
        Server server = new Server();
        server.setDatabaseName(0, DB_NAME);
        server.setDatabasePath(0, "mem:" + DB_NAME);
        server.setPort(PORT); // this is the default port
        server.start();

        blinkDataSource = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(JDBC_URL)
                .setAsyncInitIdleConnections(false));
        Assertions.assertEquals(10, blinkDataSource.getPool().getConnectionQueue().size());
    }

    @Test
    void insertAndSelect() throws SQLException {
        String value = "Hello World";
        QueryRunner queryRunner = new QueryRunner();
        try (Connection connection = blinkDataSource.getConnection()) {
            queryRunner.insert(connection, "CREATE TABLE IF NOT EXISTS db_tab (id int, name varchar(200))", new MapHandler());
            queryRunner.insert(connection, "insert into db_tab values (1, '" + value +"')", new MapHandler());
            List<Map<String, Object>> results = queryRunner.query(connection, "select id, name from db_tab", new MapListHandler());
            Assertions.assertEquals(1, results.size());
            Assertions.assertEquals(value, results.get(0).get("name"));
        }
        log.info("测试 HSQLDB 数据库完成!");
    }

    @Test
    void creatDatabase() throws SQLException {
        int minIdle = 5;
        int maxSize = 20;
        BlinkDataSource database = new BlinkDataSource(new BlinkConfig()
                .setJdbcUrl(JDBC_URL)
                .setDriverClassName(DRIVER)
                .setMinIdle(minIdle)
                .setMaxPoolSize(maxSize)
                .setAsyncInitIdleConnections(false));
        Assertions.assertEquals(minIdle, database.getConfig().getMinIdle());
        Assertions.assertEquals(maxSize, database.getConfig().getMaxPoolSize());
        Assertions.assertEquals(minIdle, database.getCurrentPoolSize());

        // 测试连接的使用情况.
        Connection connection = database.getConnection();
        Assertions.assertEquals(minIdle - 1, database.getCurrentPoolSize());
        // 归还连接.
        Assertions.assertInstanceOf(BlinkConnection.class, connection);
        connection.close();
        Assertions.assertEquals(minIdle, database.getCurrentPoolSize());

        // 关闭数据源.
        database.close();
    }

    @AfterAll
    static void destroy() {
        if (blinkDataSource != null) {
            blinkDataSource.close();
            Assertions.assertEquals(0, blinkDataSource.getCurrentPoolSize());
            Assertions.assertTrue(blinkDataSource.getPool().getConnectionQueue().isEmpty());
        }
    }

}