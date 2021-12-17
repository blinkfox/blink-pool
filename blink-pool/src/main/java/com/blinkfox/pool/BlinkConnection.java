package com.blinkfox.pool;

import com.blinkfox.pool.kit.StringKit;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于代理实现原生 JDBC 连接的连接对象.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Slf4j
public class BlinkConnection implements Connection {

    /**
     * 连接池对象.
     */
    @Getter
    private final BlinkPool pool;

    /**
     * 连接池对象.
     */
    @Getter
    private final BlinkConfig config;

    /**
     * 原生的 JDBC 连接对象.
     */
    @Getter
    private final Connection connection;

    /**
     * 最后对本连接进行检查的时间戳.
     */
    private long lastCheckTime;

    /**
     * 本连接的最终过期时间.
     */
    private final long expirationTime;

    /**
     * 最近借用本连接的纳秒时间.
     */
    @Getter
    @Setter
    private long lastBorrowNanoTime;

    /**
     * 构造方法.
     *
     * @param pool 连接池对象
     * @param connection 原生的 JDBC 连接对象
     */
    public BlinkConnection(BlinkPool pool, Connection connection) {
        this.pool = pool;
        this.connection = connection;
        this.config = pool.getConfig();

        // 为了防止大多数连接同时失效，使得每个连接的过期时间不一样，
        // 这里采用了随机生成一个范围内的过期时间，范围区间为 [maxLife * 0.8, maxLife).
        this.expirationTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(
                this.config.getMaxLifetime() * 4 / 5, this.config.getMaxLifetime());
    }

    /**
     * 关闭连接，本方法实际上会将原生的 JDBC 连接归还到连接池中，不会真的去关闭连接.
     *
     * @throws SQLException SQL 异常
     */
    @Override
    public void close() throws SQLException {
        // 统计连接借用(使用)时间.
        long closeNanoTime =  System.nanoTime();
        this.pool.getLastActiveNanoTime().lazySet(closeNanoTime);
        long diffNanoTime = closeNanoTime - this.lastBorrowNanoTime;
        if (diffNanoTime > 0) {
            this.pool.getStats().getUsedSumNanoTime().add(diffNanoTime);
        }

        // 归还连接到连接池中.
        this.pool.getBorrowing().decrement();
        this.pool.returnConnection(this);
    }

    /**
     * 真正意义上的关闭该数据库连接.
     *
     * @throws SQLException SQL 异常
     */
    public void closeReally() throws SQLException {
        this.connection.close();
        this.pool.getStats().getRealCloseds().increment();
    }

    /**
     * 安静的关闭该数据库连接.
     */
    public void closeQuietly() {
        try {
            this.closeReally();
        } catch (Exception e) {
            // 无须抛出异常，最多仅打印简单 debug 信息即可.
            if (log.isDebugEnabled()) {
                log.debug("[blink-pool 提示] 关闭数据库连接时失败，错误描述: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查当前数据库连接是否可用.
     *
     * @return 布尔值.
     * @throws SQLException SQL 异常
     */
    public boolean isAvailable() throws SQLException {
        long now = System.currentTimeMillis();
        // 如果当前连接的总存活时间超过了设置的过期时间，就视为无效了.
        if (now >= this.expirationTime) {
            return false;
        }

        // 如果检查的时间间隔小于 0, 则表示不进行检查，直接返回 true.
        long checkInterval = this.config.getCheckInterval();
        if (checkInterval < 0) {
            return true;
        }

        // 如果检查的时间间隔等于 0, 则表示始终都进行检查，直接执行检查连接有效性的方法，并返回结果.
        if (checkInterval == 0) {
            return this.doCheckValid();
        }

        // 如果当前的时间与上次的时间差小于了配置的检查时间间隔，那么就不进行检查，直接视为有效.
        if ((now - this.lastCheckTime) < checkInterval) {
            return true;
        }

        // 否则，进行检查，并设置最后的检查时间.
        boolean checkResult = this.doCheckValid();
        this.lastCheckTime = System.currentTimeMillis();
        return checkResult;
    }

    /**
     * 真正检查连接是否有效的方法.
     *
     * @return 布尔值
     * @throws SQLException SQL 异常
     */
    private boolean doCheckValid() throws SQLException {
        String checkSql = this.config.getCheckSql();
        if (StringKit.isBlank(checkSql)) {
            return !this.connection.isClosed() && this.connection.isValid(this.config.getCheckTimeout());
        }

        // 模拟一次 SQL 查询，如果查询无任何异常，则视为有效，直接返回 true，否则返回 false.
        try (PreparedStatement statement = this.connection.prepareStatement(checkSql);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.close();
            statement.close();
            return true;
        } catch (Exception e) {
            log.warn("[blink-pool 警告] 执行检查连接是否有效的 SQL 失败，将认为本链接已无效，异常原因: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return this.connection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.connection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return this.connection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        this.connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        this.connection.rollback();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        this.connection.rollback(savepoint);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.connection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.connection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        this.connection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return this.connection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        this.connection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return this.connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.connection.clearWarnings();
    }

    @Override
    public Statement createStatement() throws SQLException {
        return this.connection.createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return this.connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return this.connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return this.connection.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return this.connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return this.connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return this.connection.prepareStatement(sql, columnNames);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return this.connection.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return this.connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.connection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.connection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return this.connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return this.connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return this.connection.setSavepoint(name);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.connection.releaseSavepoint(savepoint);
    }

    @Override
    public Clob createClob() throws SQLException {
        return this.connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return this.connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return this.connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return this.connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return this.connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.connection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.connection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return this.connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return this.connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return this.connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return this.connection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return this.connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        this.connection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return this.connection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.connection.isWrapperFor(iface);
    }

}
