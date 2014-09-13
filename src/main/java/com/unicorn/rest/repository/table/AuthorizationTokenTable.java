package com.unicorn.rest.repository.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;

@Singleton
public interface AuthorizationTokenTable extends Table {
    
    public static final String AUTHORIZATION_TOKEN_TABLE_NAME = "AUTHORIZATION_TOKEN_TABLE";
    /**
     * Persist authorization token 
     * 
     * @param authorizationToken @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if authorization token with same token type and token value already exists
     * @throws RepositoryServerException internal server error
     */
    public void persistToken(@Nullable AuthorizationToken authorizationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Revoke authorization token for principal
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @param principal @Nullable
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authorization token attempted to revoke does not exist
     * @throws RepositoryServerException internal server error
     */
    public void revokeTokenForPrincipal(@Nullable AuthorizationTokenType tokenType, @Nullable String token, @Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get authorization token by looking up token type and token value
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authorization token attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull AuthorizationToken getToken(@Nullable AuthorizationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get authorization token for principal by looking up token type and token value
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @param principal @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authorization token attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull AuthorizationToken getTokenForPrincipal(@Nullable AuthorizationTokenType tokenType, @Nullable String token, @Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Delete expired authorization token
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @throws ValidationException
     * @throws ItemNotFoundException
     * @throws RepositoryServerException
     */
    public void deleteExpiredToken(@Nullable AuthorizationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
