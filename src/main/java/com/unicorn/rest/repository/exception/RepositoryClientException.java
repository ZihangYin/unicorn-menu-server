package com.unicorn.rest.repository.exception;

public class RepositoryClientException extends Exception {

    private static final long serialVersionUID = 213005503575606811L;

    public RepositoryClientException(String errMsg) {
        super(errMsg);
    }
}
