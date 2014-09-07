package com.unicorn.rest.activities.exception;

public class InvalidRequestException extends BadRequestException {

    private static final long serialVersionUID = -7956576416571951905L;
    
    private static final String ERROR_CODE = "invalid_request";
    private static final String ERROR_DESCRIPTION_FORMATTER = "The request is invalid due to missing or malformed parameters provided: %s.";

    public InvalidRequestException(Throwable cause) {
        super(ERROR_CODE, String.format(ERROR_DESCRIPTION_FORMATTER, cause.getMessage()));
    }
}
