package com.unicorn.rest.repository.exception;

public class ValidationException extends RepositoryClientException {
    
    private static final long serialVersionUID = -7603583541251298860L;
   
    private static final String ERROR_DESCRIPTION = "Request failed due to reasons such as missing required parameters, values out of range, or mismatched data types";
    
    public ValidationException() {
        super(ERROR_DESCRIPTION);
    }
    
    public ValidationException(String errMsg) {
        super(errMsg);
    }
}
