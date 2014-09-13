package com.unicorn.rest.repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.PrincipalAuthorizationInfo;

public interface AuthorizationRepository {
    
    /**
     * Get the principal for login_name
     * @param loginName @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if login_name attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getPrincipalForLoginName(@Nullable String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the principal_authorization_info for principal
     * @param principal @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if user_principal attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull PrincipalAuthorizationInfo getAuthorizationInfoForPrincipal(@Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
