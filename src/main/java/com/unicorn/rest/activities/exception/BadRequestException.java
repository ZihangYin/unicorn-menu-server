package com.unicorn.rest.activities.exception;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 3590068626650050213L;
    public static final String BAD_REQUEST = "Bad Request";
    
    private final String errorType;
    private final String errorCode;
    private final String errorDescription;
    
    public BadRequestException(String errorCode, String errorDescription) {
        super();
        this.errorType = this.getClass().getSimpleName();
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    
    public String getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
    
    @Override
    public String toString() {
        return "BadRequest [exceptionType=" + errorType
                + ", errorCode=" + errorCode + ", errorDescription="
                + errorDescription + "]";
    }
    
    @Override
    public String getMessage() {
        return "[" + errorCode + "] " + errorDescription;
    }
}
