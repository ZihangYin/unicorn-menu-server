package com.unicorn.rest.repository;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.Name;

public interface UserRepository extends AuthenticationRepository {
    
    /**
     * Generate universally unique user_principal using SimpleflakeKeyGenerator
     * Create user_principal with password and user_display_name into the USER_PROFILE table
     * Then create the user_name to user_principal mapping in the NAME_TO_PRINCIPAL_TABLE 
     * 
     * Note: If the creation of user_principal succeeded but later the creation of user_name to 
     * user_principal mapping failed due to either user_name already exists or other internal failures,
     * We would have non-associated user_principal record in the USER_PROFILE table, however, this
     * would not prevent users from retrying the registration with same user_name
     * 
     * TODO: Add a sweeper to remove non-associated user_principal record
     * 
     * @param userName @Nullable
     * @param userDisplayName @Nullable
     * @param password @Nullable
     * @return
     * @throws ValidationException
     * @throws DuplicateKeyException
     * @throws RepositoryServerException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public @Nonnull Long registerUser(@Nullable Name userName, @Nullable DisplayName userDisplayName, @Nullable String password) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException;
}
