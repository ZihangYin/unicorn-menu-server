package com.unicorn.rest.activities.exception;

public class UnrecognizedAuthorizationSchemeException extends BadRequestException {

    private static final long serialVersionUID = 983060178210388432L;
    
    private static final String ERROR_CODE = "unrecognized_authorization";
    private static final String ERROR_DESCRIPTION = "This authorization scheme is either unexpected or unsupported by the server.";
    
    public UnrecognizedAuthorizationSchemeException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
