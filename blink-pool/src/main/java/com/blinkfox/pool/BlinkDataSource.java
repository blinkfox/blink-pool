package com.blinkfox.pool;

import com.blinkfox.pool.exception.BlinkPoolException;
import com.blinkfox.pool.stat.PoolStatistics;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 连接池的数据源类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Slf4j
public class BlinkDataSource implements DataSource, Closeable {

    /**
     * 真正的连接池对象.
     */
    @Getter
    private final BlinkPool pool;

    /**
     * 连接池配置信息.
     */
    @Getter
    private final BlinkConfig config;

    /**
     * 构造方法.
     *
     * @param config 连接池配置信息
     */
    public BlinkDataSource(BlinkConfig config) {
        config.checkAndInit();
        this.config = config;
        this.pool = new BlinkPool(this.config);
        log.info("[blink-pool 提示] 创建连接池完毕，最小空闲连接数:【{}】,最大连接数:【{}】.",
                this.config.getMinIdle(), this.config.getMaxPoolSize());
    }

    /**
     * 获取数据库连接.
     *
     * @return 数据库连接
     * @throws SQLException SQL 异常
     */
    @Override
    public Connection getConnection() throws SQLException {
        final long startNanoTime = System.nanoTime();
        BlinkConnection connection;
        try {
            connection = this.pool.borrowConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BlinkPoolException("[blink-pool 异常] 获取数据库连接的线程已被中断!", e);
        }

        // 获取当前最新的时间，并将借用数 +1，
        final long endNanoTime = System.nanoTime();
        this.pool.getStats().getBorrows().increment();

        // 然后统计计算出借用连接所用的时间差，并将借用的连接数加 1.
        long diffNanoTime = endNanoTime - startNanoTime;
        if (diffNanoTime > 0) {
            this.pool.getStats().getBorrowSumNanoTime().add(diffNanoTime);
        }
        connection.setLastBorrowNanoTime(endNanoTime);
        this.pool.getLastActiveNanoTime().lazySet(endNanoTime);

        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.getConnection();
    }

    /**
     * 关闭数据库连接池.
     */
    @Override
    public void close() {
        this.pool.shutdown();
    }

    /**
     * 判断本数据源是否已关闭.
     *
     * @return 布尔值
     */
    public boolean isClosed() {
        return this.pool.isClosed();
    }

    /**
     * 获取当前连接池中的大小.
     *
     * @return 连接池大小
     */
    public int getCurrentPoolSize() {
        return this.pool.getConnectionQueue().size();
    }

    /**
     * 获取当前正在借用(使用)中的连接数.
     *
     * @return 正在借用(使用)中的连接数
     */
    public int getCurrentBorrowings() {
        return this.pool.getBorrowing().intValue();
    }

    /**
     * 获取当前连接池中的统计信息.
     *
     * @return 连接池中的统计信息
     */
    public PoolStatistics getStats() {
        return this.pool.getStats();
    }

    /**
     * 打印当前连接池中的统计信息的字符串.
     */
    public void printStats() {
        PoolStatistics stats = this.getStats();
        log.info("currBorrowings: {}, currPoolSize: {}, creations: {}, closeds: {}, borrows: {}, returns: {}, "
                        + "invalids: {}.", this.getCurrentBorrowings(), this.getCurrentPoolSize(),
                stats.getCreations(), stats.getRealCloseds(), stats.getBorrows(), stats.getReturns(),
                stats.getInvalids());
    }

    /**
     * 获取历史记录中所有创建过的数据库连接总数.
     *
     * @return 所有创建过的数据库连接总数
     */
    public long getTotalCreations() {
        return this.pool.getStats().getCreations().longValue();
    }

    /**
     * 获取历史记录中所有真实关闭过的数据库连接总数.
     *
     * @return 所有关闭过的数据库连接总数
     */
    public long getTotalRealCloseds() {
        return this.pool.getStats().getRealCloseds().longValue();
    }

    /**
     * 获取历史记录中所有从连接池中获取（借用）过的连接总数.
     *
     * @return 所有获取过的连接总数
     */
    public long getTotalBorrows() {
        return this.pool.getStats().getBorrows().longValue();
    }

    /**
     * 获取历史记录中所有从连接池中归还到连接池中的的连接总数，期间被关闭的连接不会统计在内.
     *
     * @return 所有归还过的连接总数
     */
    public long getTotalReturns() {
        return this.pool.getStats().getReturns().longValue();
    }

    /**
     * 获取历史记录中所有被检测出无效的连接总数.
     *
     * @return 所有归还过的连接总数
     */
    public long getTotalInvalids() {
        return this.pool.getStats().getInvalids().longValue();
    }

    /**
     * 获取历史记录中所有从连接池中获取过连接的累计总时间，单位毫秒 (ms).
     *
     * @return 获取连接累计的总时间
     */
    public double getBorrowsSumMillis() {
        return this.pool.getStats().getBorrowSumNanoTime().longValue() / 1000000d;
    }

    /**
     * 获取历史记录中连接使用时间的累计总时间，单位毫秒 (ms).
     *
     * @return 连接使用时间的累计总时间
     */
    public double getUsedSumMillis() {
        return this.pool.getStats().getUsedSumNanoTime().longValue() / 1000000d;
    }

    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return DriverManager.getDrivers().nextElement().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        throw new SQLException("[blink-pool 异常] 该 [" + iface + "] 接口不是本 blink-pool 的数据源!");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

}
