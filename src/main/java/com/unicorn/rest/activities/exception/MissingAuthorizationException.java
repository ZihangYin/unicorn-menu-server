package com.unicorn.rest.activities.exception;

public class MissingAuthorizationException extends BadRequestException {

    private static final long serialVersionUID = -6118015887270506647L;
    
    private static final String ERROR_CODE = "missing_authorization";
    private static final String ERROR_DESCRIPTION =  "Authorization and/or authentication failed due to no authentication information provided.";
            
    public MissingAuthorizationException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
