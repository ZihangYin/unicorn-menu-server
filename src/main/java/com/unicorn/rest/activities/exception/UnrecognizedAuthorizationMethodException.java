package com.unicorn.rest.activities.exception;

public class UnrecognizedAuthorizationMethodException extends BadRequestException {

    private static final long serialVersionUID = 983060178210388432L;
    
    private static final String ERROR_CODE = "unrecognized_authorization";
    private static final String ERROR_DESCRIPTION = "This authentication method is either unexpected or unsupported by the server.";
    
    public UnrecognizedAuthorizationMethodException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
