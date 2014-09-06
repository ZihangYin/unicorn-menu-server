package com.unicorn.rest.activities.exception;

public class InvalidRequestException extends BadRequestException {

    private static final long serialVersionUID = -7956576416571951905L;
    
    private static final String ERROR_CODE = "invalid_request";
    private static final String ERROR_DESCRIPTION = "The request is invalid due to missing or malformed parameters provided.";

    public InvalidRequestException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }

}
