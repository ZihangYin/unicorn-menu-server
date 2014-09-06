package com.unicorn.rest.activities.exception;

public class InternalServerErrorException extends RuntimeException {

    private static final long serialVersionUID = 4947832629382273858L;
    
    public static final String INTERNAL_FAILURE = "Internal Failure";
    private static final String ERROR_CODE = "internal_failure";
    private static final String ERROR_DESCRIPTION = "The server encountered an internal error while attempting to fulfill the request";
    
    private final String errorCode;
    private final String errorDescription;
    
    public InternalServerErrorException(Throwable cause) {
        super(cause);
        this.errorCode = ERROR_CODE;
        this.errorDescription = ERROR_DESCRIPTION;
    }
    
    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
    
    @Override
    public String toString() {
        return "InternalServerError [errorCode=" + errorCode
                + ", errorDescription="
                + errorDescription + ", cause=" + getCause() + "]";
    }
    
    @Override
    public String getMessage() {
        return "[" + errorCode + "] " + errorDescription;
    }
}
