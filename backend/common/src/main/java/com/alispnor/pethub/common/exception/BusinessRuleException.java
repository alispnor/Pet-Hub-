package com.alispnor.pethub.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @Override
    public String getErrorType() {
        return "business-rule-violation";
    }
}
