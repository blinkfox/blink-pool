package com.blinkfox.pool;

import com.blinkfox.pool.exception.BlinkPoolException;
import com.blinkfox.pool.kit.StringKit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * 连接池配置类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Slf4j
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class BlinkConfig {

    /**
     * 默认的连接池名称.
     */
    public static final String DEFAULT_POOL_NAME = "blink-pool";

    /**
     * 一个简单的用于检查连接是否有效的 SQL 语句.
     *
     * <p>注意：除非特殊情况不建议设置 {@link #checkSql} 的属性值.</p>
     */
    public static final String SIMPLE_CHECK_SQL = "SELECT 1";

    /**
     * 默认的连接池大小.
     */
    public static final int DEFAULT_MIN_POOL_SIZE = 10;

    /**
     * 默认的最大连接池大小.
     */
    public static final int DEFAULT_MAX_POOL_SIZE = 20;

    /**
     * 连接池中多余连接的最小闲置时间，秒（s）.
     */
    public static final int MIN_IDLE_TIMEOUT = 10;

    /**
     * 连接池中多余连接的默认闲置时间，秒（s）.
     */
    public static final int DEFAULT_IDLE_TIMEOUT = 60;

    /**
     * 连接池中检查连接是否有效的最小时间间隔常量，单位毫秒（ms）.
     */
    public static final int MIN_CHECK_INTERVAL = 500;

    /**
     * 默认的检查连接是否有效的时间间隔，单位毫秒 (ms).
     */
    public static final int DEFAULT_CHECK_INTERVAL = 2000;

    /**
     * 最小的最长连接存活时长，单位毫秒 (ms).
     */
    public static final int MIN_LIFE_TIME = 60000;

    /**
     * 默认的最长连接存活时长，单位毫秒 (ms).
     */
    public static final int DEFAULT_LIFE_TIME = 1800000;

    /**
     * 检查连接是否有效的默认超时时间，单位秒 (s).
     */
    public static final int DEFAULT_CHECK_TIMEOUT = 5;

    /**
     * 检查连接是否有效的最小超时时间，单位秒 (s).
     */
    public static final int MIN_CHECK_TIMEOUT = 1;

    /**
     * 默认的借用连接的超时时间常量，单位毫秒 (ms).
     */
    public static final int DEFAULT_BORROW_TIMEOUT = 30000;

    /**
     * 连接池的名称.
     */
    private String poolName = DEFAULT_POOL_NAME;

    /**
     * JDBC 的 URL 地址.
     */
    private String jdbcUrl;

    /**
     * JDBC 连接驱动的 class 名称.
     *
     * <p>自动基于 jdbc URL 来识别 driver class name 的值，如果是很新或者很老的驱动，建议自己手动设置该参数值.</p>
     */
    private String driverClassName;

    /**
     * 数据库连接的用户名.
     */
    private String username;

    /**
     * 数据库连接的密码.
     */
    private String password;

    /**
     * 连接池最小空闲连接数.
     */
    private int minIdle = DEFAULT_MIN_POOL_SIZE;

    /**
     * 连接池最大连接数.
     */
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    /**
     * 允许多余的连接在池中闲置的最长时间，单位秒（s）.
     */
    private int idleTimeout;

    /**
     * 连接的最长存活时间，单位毫秒 (ms).
     *
     * <p>默认 30 分钟，最少 1 分钟.</p>
     */
    private long maxLifetime = DEFAULT_LIFE_TIME;

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
    private long checkInterval = DEFAULT_CHECK_INTERVAL;

    /**
     * 检查数据库连接是否有效时的超时时间，单位秒 (s), 默认是 5 秒钟，最少可配置 1 秒钟.
     */
    private int checkTimeout = DEFAULT_CHECK_TIMEOUT;

    /**
     * 用于设置检查连接是否有效的 SQL.
     *
     * <p>通常对于支持 JDBC4 的驱动不要设置此 SQL 值，除非你的 JDBC 驱动不是 JDBC4 或者不支持 {@code isValid()} 方法。
     * 通常，检查连接是否有效的 SQL 应该极为精简快速的才行，如：{@code 'select 1'}。
     * 由于该检查 SQL 效率相比于驱动自身的 {@code isValid()} 方法较低，如果使用此参数，
     * 建议你再把 {@link #checkInterval} 的间隔值设置大一点.</p>
     */
    private String checkSql;

    /**
     * 从连接池中借用连接的超时时间，单位毫秒 (ms)，默认为 30 秒钟.
     *
     * <p>如果正在被借用（使用）中的连接数大于等于了最大连接数，此时再从数据库连接池中借用连接时会阻塞，
     * 直到从连接池中获取到连接为止，如果在本超时时间之内还没有获取到连接，那么就直接抛出异常.</p>
     */
    private long borrowTimeout = DEFAULT_BORROW_TIMEOUT;

    /**
     * 检查配置信息是否正确，并做对应的初始化处理.
     */
    public void checkAndInit() {
        // 检查和初始化 jdbc URL 信息.
        if (StringKit.isBlank(this.jdbcUrl)) {
            throw new IllegalArgumentException("[blink-pool 异常] 连接池参数 jdbcUrl 不能为空！");
        }

        // 检查和初始化连接池的基础信息.
        this.checkAndInitDriverClassName();
        this.checkAndInitPoolSize();

        // 检查和初始化闲置时间 idleTime、maxLifetime 等值.
        this.checkAndInitIdleTime();
        this.checkAndInitMaxLifeTime();
        this.checkAndInitOtherOptions();
    }

    /**
     * 检查和初始化 JDBC 连接的驱动类 class 的信息.
     */
    private void checkAndInitDriverClassName() {
        // 如果人工设置了驱动类，就直接跳过本方法.
        if (StringKit.isNotBlank(this.driverClassName)) {
            return;
        }

        // 否则，根据 JDBC URL 来自动识别主流的数据库的驱动类 class.
        if (this.jdbcUrl.startsWith("jdbc:")) {
            String[] arr = this.jdbcUrl.split(":", 4);
            if (arr.length < 2) {
                throw new BlinkPoolException("[blink-pool 异常] 配置的 jdbcUrl 参数格式不正确！");
            }
            this.driverClassName = this.getDriverClassNameByType(arr[1]);
        }

        // 如果最终的 driverClassName 还是为空，就直接抛异常提示设置.
        if (StringKit.isBlank(this.driverClassName)) {
            throw new IllegalArgumentException("[blink-pool 异常] 连接池参数 driverClassName 为空，请手动设置该配置项的值！");
        }
    }

    /**
     * 根据数据库类型名称获取对应的驱动 class 名称.
     *
     * @param type 类型
     * @return class 名称
     */
    private String getDriverClassNameByType(String type) {
        switch (type) {
            case "postgresql":
                return "org.postgresql.Driver";
            case "mysql":
                return "com.mysql.jdbc.Driver";
            case "hsqldb":
                return "org.hsqldb.jdbc.JDBCDriver";
            case "h2":
                return "org.h2.Driver";
            case "oracle":
                return "oracle.jdbc.driver.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "sybase":
                return "com.sybase.jdbc3.jdbc.SybDriver";
            case "db2":
                return "com.ibm.db2.jdbc.app.DB2Driver";
            case "jtds":
                return "net.sourceforge.jtds.jdbc.Driver";
            case "kingbase8":
                return "com.kingbase8.Driver";
            case "uxdb":
                return "com.uxsino.uxdb.Driver";
            case "dm":
                return "dm.jdbc.driver.DmDriver";
            case "informix-sqli":
                return "com.informix.jdbc.IfxDriver";
            case "log4jdbc":
                return "net.sf.log4jdbc.sql.jdbcapi.DriverSpy";
            default:
                return null;
        }
    }

    /**
     * 检查和初始化连接池的大小信息.
     */
    private void checkAndInitPoolSize() {
        // 设置最大和最小连接数.
        if (this.minIdle < 0) {
            throw new IllegalArgumentException(StringKit.format(
                    "[blink-pool 异常] 最小空闲连接数 minIdle 的值必须大于 0，当前值为:【{}】！", this.minIdle));
        }
        if (this.maxPoolSize < 0) {
            throw new IllegalArgumentException(StringKit.format(
                    "[blink-pool 异常] 最大连接数 maxPoolSize 的值必须大于 0，当前值为:【{}】！", this.maxPoolSize));
        }
        this.minIdle = this.minIdle == 0 ? DEFAULT_MIN_POOL_SIZE : this.minIdle;
        this.maxPoolSize = this.maxPoolSize == 0 ? DEFAULT_MAX_POOL_SIZE : this.maxPoolSize;

        // 如果最小最大值不对，可能是人工设置有误，直接交换这两个值.
        if (this.minIdle > this.maxPoolSize) {
            int temp = this.maxPoolSize;
            this.maxPoolSize = this.minIdle;
            this.minIdle = temp;
            log.warn("[blink-pool 警告] 检测到最小闲置连接数 minIdle 的值【{}】大于了最大连接数 maxPoolSize 的值【{}】，"
                    + "可能是人工设置有误，将自动交换这两个值.", this.minIdle, this.maxPoolSize);
        }
    }

    /**
     * 检查和初始化闲置时间 idleTime 的值.
     *
     * <p>设置允许多余的连接在池中闲置的的最长时间.
     * 如果该值小于0，就抛出异常；如果该值等于 0，就设置为默认值；如果该值不等于 0 且小于 10，就设置默认最小值 10.</p>
     */
    private void checkAndInitIdleTime() {
        if (this.idleTimeout < 0) {
            throw new IllegalArgumentException(StringKit.format(
                    "[blink-pool 异常] 允许多余的连接在池中闲置的的最长时间 idleTimeout 的值必须大于0，当前值为:【{}】！", this.idleTimeout));
        }
        this.idleTimeout = this.idleTimeout == 0 ? DEFAULT_IDLE_TIMEOUT : this.idleTimeout;
        this.idleTimeout = Math.max(this.idleTimeout, MIN_IDLE_TIMEOUT);
    }

    /**
     * 检查和初始化连接的最长存活时间 maxLifetime 的值.
     */
    private void checkAndInitMaxLifeTime() {
        if (this.maxLifetime < 0) {
            throw new IllegalArgumentException(StringKit.format(
                    "[blink-pool 异常] 连接池中连接的最长存活时间 maxLifetime 的值必须大于 0，当前值为:【{}】！", this.maxLifetime));
        }
        this.maxLifetime = this.maxLifetime == 0 ? DEFAULT_LIFE_TIME : this.maxLifetime;
        this.maxLifetime = this.maxLifetime < MIN_LIFE_TIME ? MIN_LIFE_TIME : this.maxLifetime;
    }

    /**
     * 检查和初始化其他相关的参数.
     */
    private void checkAndInitOtherOptions() {
        // 设置连接检查的最小间隔时间设置项.
        this.checkInterval = this.checkInterval > 0 && this.checkInterval < MIN_CHECK_INTERVAL
                ? MIN_CHECK_INTERVAL
                : this.checkInterval;

        // 设置检查的超时时间.
        this.checkTimeout = this.checkTimeout < 0 ? DEFAULT_CHECK_TIMEOUT : this.checkTimeout;
        this.checkTimeout = Math.max(this.checkTimeout, MIN_CHECK_TIMEOUT);

        // 借用超时时间.
        this.borrowTimeout = this.borrowTimeout <= 0 ? DEFAULT_BORROW_TIMEOUT : this.borrowTimeout;
    }

}
