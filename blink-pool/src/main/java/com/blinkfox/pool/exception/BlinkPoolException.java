package com.blinkfox.pool.exception;

/**
 * 自定义的连接池运行时异常.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
public class BlinkPoolException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法.
     *
     * @param msg 日志消息
     */
    public BlinkPoolException(String msg) {
        super(msg);
    }

    /**
     * 附带日志消息参数的构造方法.
     *
     * @param msg 日志消息
     * @param t Throwable对象
     */
    public BlinkPoolException(String msg, Throwable t) {
        super(msg, t);
    }

}
