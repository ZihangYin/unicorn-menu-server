package com.unicorn.rest.activities.exception;

import javax.annotation.Nonnull;

import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrCode;

public class OAuthBadRequestException extends BadRequestException {

    private static final long serialVersionUID = 204677130416245925L;
    
    public OAuthBadRequestException(@Nonnull OAuthErrCode errorCode, @Nonnull String errorDescription) {
        super(errorCode.toString(), errorDescription);
    }
}
