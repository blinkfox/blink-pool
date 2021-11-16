package com.blinkfox.pool.stat;

import java.util.concurrent.atomic.LongAdder;
import lombok.Getter;
import lombok.ToString;

/**
 * 本连接池类库中用来统计 JDBC 连接池相关数据的统计类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@Getter
@ToString
public class PoolStatistics {

    /**
     * 用于统计创建真实连接的总记录数.
     */
    private final LongAdder creations;

    /**
     * 被真实关闭过的数据库连接数.
     */
    private final LongAdder realCloseds;

    /**
     * 用于统计已从连接池中借用的连接总数.
     */
    private final LongAdder borrows;

    /**
     * 用于统计已归还到连接池中的连接总数.
     */
    private final LongAdder returns;

    /**
     * 被检查出来的无效连接数.
     */
    private final LongAdder invalids;

    /**
     * 用于累加借用数据库连接操作所用的纳秒时间总和，可以用来统计某个时间段内连接的平均获取时间.
     */
    private final LongAdder borrowSumNanoTime;

    /**
     * 用于累加使用数据库连接操作所用的纳秒时间总和，可以用来统计某个时间段内连接的平均使用时间.
     */
    private final LongAdder usedSumNanoTime;

    /**
     * 构造方法.
     */
    public PoolStatistics() {
        this.creations = new LongAdder();
        this.realCloseds = new LongAdder();
        this.borrows = new LongAdder();
        this.returns = new LongAdder();
        this.invalids = new LongAdder();
        this.borrowSumNanoTime = new LongAdder();
        this.usedSumNanoTime = new LongAdder();
    }

    /**
     * 用来修复数据的方法，处理可能超出 long 数据最大值之后，为负数的情况.
     */
    public void fixData() {
        if (this.creations.longValue() < 0) {
            this.creations.reset();
        }
        if (this.realCloseds.longValue() < 0) {
            this.realCloseds.reset();
        }
        if (this.borrows.longValue() < 0) {
            this.borrows.reset();
        }
        if (this.returns.longValue() < 0) {
            this.returns.reset();
        }
        if (this.invalids.longValue() < 0) {
            this.invalids.reset();
        }
        if (this.borrowSumNanoTime.longValue() < 0) {
            this.borrowSumNanoTime.reset();
        }
        if (this.usedSumNanoTime.longValue() < 0) {
            this.usedSumNanoTime.reset();
        }
    }

    /**
     * 重置所有数据为 0 值.
     */
    public void resetAll() {
        this.creations.reset();
        this.realCloseds.reset();
        this.borrows.reset();
        this.returns.reset();
        this.invalids.reset();
        this.borrowSumNanoTime.reset();
        this.usedSumNanoTime.reset();
    }

}
