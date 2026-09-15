package com.mall.pointsmall.exception;

/**
 * An expected credential failure whose attempt counter must survive transaction completion.
 */
public class LoginFailureException extends BusinessException {
    public LoginFailureException(String message) {
        super(message);
    }
}
