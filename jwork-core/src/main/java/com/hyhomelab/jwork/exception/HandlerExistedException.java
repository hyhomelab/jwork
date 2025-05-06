package com.hyhomelab.jwork.exception;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 23:03
 */
public class HandlerExistedException extends RuntimeException{

    public HandlerExistedException() {
    }

    public HandlerExistedException(String message) {
        super(message);
    }

    public HandlerExistedException(String message, Throwable cause) {
        super(message, cause);
    }

    public HandlerExistedException(Throwable cause) {
        super(cause);
    }

    public HandlerExistedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
