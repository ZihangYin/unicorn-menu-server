package com.unicorn.rest.repository;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.repository.model.UserName;

public interface UserRepository extends AuthenticationRepository {
    
    /**
     * Generate globally unique user_id using SimpleflakeKeyGenerator
     * Create user_id with password and user_display_name into the USER_PROFILE table
     * Then create the user_name to user_id mapping in the USER_NAME_TO_ID_TABLE 
     * 
     * Note: If the creation of user_id succeeded but later the creation of user_name to 
     * user_id mapping failed due to either user_name already exists or other internal failures,
     * We would have non-associated user_id record in the USER_PROFILE table, however, this
     * would not prevent users to retry the registration with same user_name
     * 
     * TODO: Add a sweeper to remove non-associated user_id record
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
    public @Nonnull Long registerUser(@Nullable UserName userName, @Nullable UserDisplayName userDisplayName, @Nullable String password) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException;
}
