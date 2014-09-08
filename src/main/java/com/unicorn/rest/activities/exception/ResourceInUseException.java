package com.unicorn.rest.activities.exception;

public class ResourceInUseException extends BadRequestException {

    private static final long serialVersionUID = 8958757181142017462L;
    
    private static final String ERROR_CODE = "resource_in_use";
    private static final String ERROR_DESCRIPTION = "The request failed due to attempt to create reousrce already existed or delete one not existed.";
    
    public ResourceInUseException() {
        super(ERROR_CODE, ERROR_DESCRIPTION);
    }
}
