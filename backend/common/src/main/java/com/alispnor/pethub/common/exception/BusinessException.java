package com.alispnor.pethub.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de todas as exceptions de negócio do Pet Hub.
 * Subclasses devem indicar o {@link HttpStatus} apropriado via {@link #getStatus()}.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract HttpStatus getStatus();

    /**
     * Identificador estável do tipo do erro, usado no campo "type" do RFC 7807.
     * Ex.: "resource-not-found", "business-rule-violation".
     */
    public abstract String getErrorType();
}
