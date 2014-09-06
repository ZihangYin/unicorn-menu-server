package com.unicorn.rest.repository.exception;

public class StaleDataException extends RepositoryServerException {

    private static final long serialVersionUID = -544873159217602148L;
    
    private static final String ERROR_MESSAGE = "The repository detected inconsistent state while attempting to fulfill the request";

    public StaleDataException() {
        super(ERROR_MESSAGE);
    }

    
}
