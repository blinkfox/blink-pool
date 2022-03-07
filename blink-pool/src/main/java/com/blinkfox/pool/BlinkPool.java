package com.blinkfox.pool;

import com.blinkfox.pool.exception.BlinkPoolException;
import com.blinkfox.pool.stat.PoolStatistics;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 本类库中用来管理 JDBC 的连接池类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Slf4j
class BlinkPool {

    /**
     * 循环定时检查是否有多余空闲连接的时间间隔秒数.
     */
    private static final int CHECK_PERIOD_TIME_SECONDS = 5;

    /**
     * 连接池配置信息.
     */
    @Getter
    private final BlinkConfig config;

    /**
     * 连接池监控统计信息.
     */
    @Getter
    private final PoolStatistics stats;

    /**
     * 本链接池是否已关闭.
     *
     * <p>注：此处使用原始的 boolean 类型，只有在关闭线程池时才会修这个值，可能有线程安全问题。
     * 但实际场景中手动关闭连接的概率极小，对系统影响几乎可以忽略，所以，为了性能，直接使用原始类型即可.</p>
     */
    @Getter
    private boolean closed;

    /**
     * 连接池中连接的最后活动时间，包括了借用或归还等操作的最后操作时间，用于计算连接池中的连接是否闲置了较长时间.
     *
     * @since 1.0.1
     */
    @Getter
    private final AtomicLong lastActiveNanoTime;

    /**
     * 从连接池中正在被借用中的连接数，用来表示正在被外部使用的连接数.
     */
    @Getter
    private final LongAdder borrowing;

    /**
     * 用来存放数据库连接的阻塞队列.
     */
    @Getter
    private final BlockingQueue<BlinkConnection> connectionQueue;

    /**
     * 用来表示定时清除多余空闲连接的定时调度器.
     */
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * 用于判断是否可创建创建数据库连接时的锁.
     *
     * @since 1.0.1
     */
    private final Lock createConnLock;

    /**
     * 构造方法.
     *
     * @param blinkConfig 连接池配置类 {@link BlinkPool} 的实例对象
     */
    public BlinkPool(BlinkConfig blinkConfig) {
        this.config = blinkConfig;
        this.stats = new PoolStatistics();
        this.borrowing = new LongAdder();
        this.lastActiveNanoTime = new AtomicLong(System.nanoTime());
        this.createConnLock = new ReentrantLock();
        this.connectionQueue = new ArrayBlockingQueue<>(blinkConfig.getMaxPoolSize());

        // 先加载驱动类的 class，然后在初始化创建数据库连接池中的最小空闲连接.
        this.loadDriverClass();

        // 初始化连接池中的空闲连接.
        this.initCreateIdleConnections();

        // 创建最小连接成功之后，再创建定时任务的线程池，
        // 用于维持连接池最小闲置连接数的定时任务，对于较多的连接需要清除，对于较少的连接需要创建.
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(
                1, r -> new Thread(r, "blink-pool"),
                (r, e) -> log.warn("[blink-pool 警告] 已经启动或运行了用于维持空闲连接的定时任务!"));
        try {
            this.startKeepIdleConnectionsJob();
        } catch (Exception e) {
            // 如果启动失败，则直接关闭 executor，并抛出异常.
            this.scheduledExecutor.shutdownNow();
            throw e;
        }
    }

    /**
     * 初始化创建连接池中的空闲连接.
     *
     * @since 1.0.1
     */
    private void initCreateIdleConnections() {
        // 初始化创建一个连接.
        try {
            this.createBlinkConnectionIntoPool();
        } catch (SQLException e) {
            throw new BlinkPoolException("[blink-pool 异常] 初始化创建数据库连接时发生异常！", e);
        }

        // 如果最小闲置数大于 1 的话，那么判断使用同步或异步的方式去初始化更多的初始空闲连接.
        if (this.config.getMinIdle() > 1) {
            if (this.config.isAsyncInitIdleConnections()) {
                CompletableFuture.runAsync(this::createMinIdleConnections);
            } else {
                this.createMinIdleConnections();
            }
        }
    }

    /**
     * 加载 JDBC 驱动类的 class.
     */
    private void loadDriverClass() {
        String driverClass = this.config.getDriverClassName();
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new BlinkPoolException("[blink-pool 异常] 未找到类路径名为【" + driverClass + "】的驱动类 Class.", e);
        }
    }

    /**
     * 创建出最小可用的数据库连接数.
     */
    private void createMinIdleConnections() {
        this.createConnLock.lock();
        try {
            while (this.connectionQueue.size() < this.config.getMinIdle()) {
                this.createBlinkConnectionIntoPool();
            }
        } catch (SQLException e) {
            throw new BlinkPoolException("[blink-pool 异常] 创建数据库连接时发生异常！", e);
        } finally {
            this.createConnLock.unlock();
        }
    }

    /**
     * 创建全新的数据库 JDBC 连接，并封装成 {@link BlinkConnection} 对象放入连接池中.
     *
     * <p>
     *     如果当前已有的连接总数已经大于等于了最大连接数，就不需要再创建数据库连接了.
     *     然后，创建出一个数据库连接，并尝试将其放到连接池中，如果加入到连接池中失败就需要关闭该数据库连接.
     * </p>
     */
    private void createBlinkConnectionIntoPool() throws SQLException {
        // 如果当前所有连接总数都小于最大连接数，那么就创建新的数据库连接，并放到连接池中.
        if ((this.connectionQueue.size() + this.borrowing.intValue()) < this.config.getMaxPoolSize()) {
            BlinkConnection connection = this.newBlinkConnection();
            if (!this.connectionQueue.offer(connection)) {
                connection.closeQuietly();
                log.debug("[blink-pool 提示] 连接池已满,无法再将该数据库连接放到连接池中，将直接关闭该连接！");
            }
        }
    }

    /**
     * 创建出新的 {@link BlinkConnection} 的连接，并将统计数据加 1.
     *
     * @return 连接
     * @throws SQLException SQL 异常
     */
    private BlinkConnection newBlinkConnection() throws SQLException {
        BlinkConnection connection = new BlinkConnection(this, DriverManager.getConnection(
                this.config.getJdbcUrl(), this.config.getUsername(), this.config.getPassword()));
        this.stats.getCreations().increment();
        return connection;
    }

    /**
     * 从连接池中获取一个数据库连接对象.
     *
     * <p>如果从连接池中取出的连接为空，说明连接池是空的了.
     *    这里采用不阻塞且再创建一个新的数据库连接的方式来返回该连接，并触发异步创建最小可用连接数的异步任务.
     * </p>
     *
     * @return {@link BlinkConnection} 对象
     * @throws SQLException SQL 异常
     * @throws InterruptedException 中断异常
     */
    public BlinkConnection borrowConnection() throws SQLException, InterruptedException {
        if (this.closed) {
            throw new BlinkPoolException("[blink-pool 异常] 连接池已关闭，不能再获取数据库连接！");
        }

        // 先通过从连接池中非阻塞的获取连接，如果连接为空，说明连接池是空的，那么就判断当前的正在被使用的连接数是否超过了约定的最大连接数.
        BlinkConnection connection = this.connectionQueue.poll();
        if (connection == null) {
            // 如果连接池是空的，且正在使用中的连接比最大连接数小，那么就尝试异步创建新的数据库连接即可.
            if (this.borrowing.intValue() < this.config.getMaxPoolSize() && this.connectionQueue.isEmpty()) {
                CompletableFuture.runAsync(this::createMinIdleConnections);
            }

            // 超过了最大连接数，那么就尝试阻塞获取连接，直到超时为止，如果最后获取的还是空的，那么就抛出异常.
            connection = this.connectionQueue.poll(config.getBorrowTimeout(), TimeUnit.MILLISECONDS);
            if (connection == null) {
                throw new SQLException("[blink-pool 异常] 从连接池中获取数据库连接已超时，建议调大最大连接数的配置项或者优化慢 SQL!");
            }
        }

        // 判断连接是否有效之前，先将借用中的值 +1. 如果连接是可用的有效的，就直接返回此连接.
        this.borrowing.increment();
        if (connection.isAvailable()) {
            return connection;
        }

        // 如果检查出是无效连接了，借用中的值 -1,并记录无效连接数和关闭原连接，并尝试直接创建一个新的连接.
        this.borrowing.decrement();
        this.stats.getInvalids().increment();
        connection.closeQuietly();
        connection = this.newBlinkConnection();
        this.borrowing.increment();
        return connection;
    }

    /**
     * 释放一个数据库连接到连接池中.
     *
     * <p>如果连接已经无效了或者将连接放回到连接池中失败了，就直接关闭此数据库连接.</p>
     *
     * @param connection 连接池对象.
     */
    public void returnConnection(BlinkConnection connection) throws SQLException {
        // 如果连接池已被关闭，就直接关闭此额外的连接即可.
        if (this.closed) {
            connection.closeQuietly();
            return;
        }

        // 归还连接，如果连接不能再归还到连接池中，说明了连接池已经满了，就直接关闭此连接.
        if (this.connectionQueue.offer(connection)) {
            this.stats.getReturns().increment();
        } else {
            connection.closeReally();
            if (log.isDebugEnabled()) {
                log.debug("[blink-pool 提示] 连接池已满，归还数据库连接到 blink-pool 连接池中失败，将直接关闭此连接！如果频繁报出此警告日志，"
                        + "原因可能是当前并发请求量较大，建议你增加最大连接数 maxPoolSize 的配置值。");
            }
        }
    }

    /**
     * 启动维持最小可用空闲连接的定时任务.
     */
    public void startKeepIdleConnectionsJob() {
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                doCreateOrClearIdleConnections();
            } catch (Exception e) {
                log.error("[blink-pool 错误] 定时维持连接池中的最小空闲连接时出错！", e);
            }
        }, CHECK_PERIOD_TIME_SECONDS, CHECK_PERIOD_TIME_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 创建或清除多余的空闲连接.
     */
    private void doCreateOrClearIdleConnections() {
        // 如果当前时间与连接的最后活动时间差小于了配置的闲置时间，就不会去清理空闲连接.
        long periodTime = this.config.getIdleTimeout();
        if ((System.nanoTime() - this.lastActiveNanoTime.get()) < (periodTime * 1e9)) {
            return;
        }

        // 开始尝试清理连接池中多余的空闲连接.
        if (log.isDebugEnabled()) {
            log.debug("[blink-pool 提示] 开始进入了用于维持最小可用空闲连接的定时任务方法中 ...");
        }

        // 如果有多余的连接，就循环清除多余的空闲连接.
        int minIdle = this.config.getMinIdle();
        while (this.connectionQueue.size() > minIdle) {
            BlinkConnection connection = this.connectionQueue.poll();
            if (connection != null) {
                connection.closeQuietly();
            }
        }

        // 如果连接数变少了，就再创建新的数据库连接到最小可用连接数.
        this.createMinIdleConnections();

        // 修复统计数据可能超过 long 类型的最大值之后为负数的情况.
        this.stats.fixData();
    }

    /**
     * 关闭连接池中的若干连接.
     */
    public void shutdown() {
        // 设置已关闭，并关闭定时任务的调度器.
        this.closed = true;
        this.scheduledExecutor.shutdown();

        // 循环关闭和清空连接池.
        this.connectionQueue.forEach(BlinkConnection::closeQuietly);
        this.connectionQueue.clear();
        this.stats.resetAll();
    }

}
