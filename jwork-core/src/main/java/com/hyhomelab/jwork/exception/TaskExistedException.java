package com.hyhomelab.jwork.exception;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 23:03
 */
public class TaskExistedException extends Exception{

    public TaskExistedException() {
    }

    public TaskExistedException(String message) {
        super(message);
    }

    public TaskExistedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskExistedException(Throwable cause) {
        super(cause);
    }

    public TaskExistedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
