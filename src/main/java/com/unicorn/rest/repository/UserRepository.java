package com.unicorn.rest.repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;

public interface UserRepository {
    
    /**
     * Get the user_id from login_name
     * @param loginName
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if login_name attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getUserIdFromLoginName(@Nullable String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the user_authorization_info from user_id
     * @param userId
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if user_id attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull UserAuthorizationInfo getUserAuthorizationInfo(@Nullable Long userId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
}