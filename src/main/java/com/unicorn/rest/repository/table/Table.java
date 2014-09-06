package com.unicorn.rest.repository.table;

import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;

public interface Table {
    public void createTable() throws RepositoryClientException, RepositoryServerException;
    public void deleteTable() throws RepositoryClientException, RepositoryServerException;
}
