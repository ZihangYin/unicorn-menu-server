package com.unicorn.rest.activities.exception;

import javax.annotation.Nonnull;

import com.unicorn.rest.activities.exception.TokenErrors.TokenErrCode;

public class BadTokenRequestException extends BadRequestException {

    private static final long serialVersionUID = 204677130416245925L;
    
    public BadTokenRequestException(@Nonnull TokenErrCode errorCode, @Nonnull String errorDescription) {
        super(errorCode.toString(), errorDescription);
    }
}
