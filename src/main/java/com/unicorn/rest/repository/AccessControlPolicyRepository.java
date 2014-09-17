package com.unicorn.rest.repository;

import java.util.List;

import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;

public interface AccessControlPolicyRepository {
    /**
     * Get existing access control policy for action on resource
     * 
     * @param action @Nullable
     * @param resourceIdentifier @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if access control policy attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public List<String> getAccessControlPolicy(@Nullable String action, @Nullable String resourceIdentifier) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
}
