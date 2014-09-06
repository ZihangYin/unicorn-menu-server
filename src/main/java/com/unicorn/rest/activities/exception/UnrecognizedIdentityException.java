package com.unicorn.rest.activities.exception;

public class UnrecognizedIdentityException extends BadRequestException {

    private static final long serialVersionUID = 8931757934682792196L;
    
    private static final String ERROR_CODE = "unrecognized_identity";
    private static final String ERROR_DESCRIPTION = "Authentication failed due to missing, invalid or malformed principal and/or credential.";
    
    public UnrecognizedIdentityException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
