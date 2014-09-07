package com.unicorn.rest.repository.table;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;

@Singleton
public interface UserProfileTable extends Table {
    /**
     * Create new user with required minimum parameters
     *  
     * @param userId
     * @param userDisplayName
     * @param password
     * @param salt
     * @return
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if the user_id already exists
     * @throws RepositoryServerException internal server error
     */
    public Long createUser(@Nullable Long userId, @Nullable String userDisplayName, @Nullable ByteBuffer password, @Nullable ByteBuffer salt) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Build user_authorization_info from attributes, which contains user_display_name, hashed password and salt.
     * 
     * @param userId
     * @return 
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if user_id does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull UserAuthorizationInfo getUserAuthorizationInfo(@Nullable Long userId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
