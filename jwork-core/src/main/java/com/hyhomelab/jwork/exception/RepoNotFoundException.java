package com.hyhomelab.jwork.exception;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 23:03
 */
public class RepoNotFoundException extends RuntimeException{

    public RepoNotFoundException() {
    }

    public RepoNotFoundException(String message) {
        super(message);
    }

    public RepoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepoNotFoundException(Throwable cause) {
        super(cause);
    }

    public RepoNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
