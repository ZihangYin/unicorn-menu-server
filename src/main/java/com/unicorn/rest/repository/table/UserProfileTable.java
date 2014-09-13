package com.unicorn.rest.repository.table;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;

@Singleton
public interface UserProfileTable extends Table {
    
    public static final String USER_PROFILE_TABLE_NAME = "USER_PROFILE_TABLE";
    /**
     * Create new user with required minimum parameters
     *  
     * @param userPrincipal @Nullable
     * @param userDisplayName @Nullable
     * @param password @Nullable
     * @param salt @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if the user_principal already exists
     * @throws RepositoryServerException internal server error
     */
    public Long createUser(@Nullable Long userPrincipal, @Nullable DisplayName userDisplayName, @Nullable ByteBuffer password, @Nullable ByteBuffer salt) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Build user_authorization_info from attributes, which contains user_display_name, hashed password and salt.
     * 
     * @param userPrincipal @Nullable
     * @return 
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if user_principal does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull PrincipalAuthenticationInfo getUserAuthorizationInfo(@Nullable Long userPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
