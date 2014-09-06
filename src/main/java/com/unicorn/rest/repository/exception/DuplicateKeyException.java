package com.unicorn.rest.repository.exception;

public class DuplicateKeyException extends RepositoryClientException {
    
    private static final long serialVersionUID = -2012076886085693799L;
    
    private static final String ERROR_MESSAGE = "Item cannot be created because an item with the same key already exist.";
    
    public DuplicateKeyException() {
        super(ERROR_MESSAGE);
    }
}
