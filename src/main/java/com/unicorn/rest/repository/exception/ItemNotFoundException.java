package com.unicorn.rest.repository.exception;

public class ItemNotFoundException extends RepositoryClientException {
    
    private static final long serialVersionUID = 6567819966525441574L;
    
    private static final String ERROR_MESSAGE = "The item specified in the request cannot be not be found.";
    
    public ItemNotFoundException() {
        super(ERROR_MESSAGE);
    }
}
