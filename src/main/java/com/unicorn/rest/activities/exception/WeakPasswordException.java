package com.unicorn.rest.activities.exception;

public class WeakPasswordException extends BadRequestException {
    
    private static final long serialVersionUID = -6700852851644563325L;
    
    private static final String ERROR_CODE = "weak_password";
    private static final String ERROR_DESCRIPTION = "The password must be at least 6 characters, no more than 15 characters and must have at least one numeric digit and one letter.";
    
    public WeakPasswordException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
