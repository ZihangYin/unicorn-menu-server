package com.unicorn.rest.repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;

public interface AuthenticationRepository {
    
    /**
     * Get the principal from login_name, the principal in this case is user_id
     * @param loginName @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if login_name attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getPrincipalFromLoginName(@Nullable String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the principal_authorization_info from principal
     * @param userId @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if user_id attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull PrincipalAuthenticationInfo getAuthorizationInfo(@Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
