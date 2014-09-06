package com.unicorn.rest.repository.exception;

public class RepositoryServerException extends Exception {

    private static final long serialVersionUID = 6883344758645289772L;
    
    private static final String ERROR_MESSAGE = "The repository encountered an internal error while attempting to fulfill the request";
    
    public RepositoryServerException(String errMsg) {
        super(errMsg);
    }
    
    public RepositoryServerException(Exception cause) {
        this(ERROR_MESSAGE, cause);
    }
    
    public RepositoryServerException(String errMsg, Exception cause) {
        super(errMsg, cause);
    }
}
