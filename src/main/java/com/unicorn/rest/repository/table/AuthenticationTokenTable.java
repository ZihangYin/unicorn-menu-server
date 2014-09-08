package com.unicorn.rest.repository.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;

@Singleton
public interface AuthenticationTokenTable extends Table {
    
    /**
     * Persist authentication token 
     * 
     * @param authenticationToken @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if authentication token with same token type and token value already exists
     * @throws RepositoryServerException internal server error
     */
    public void persistToken(@Nullable AuthenticationToken authenticationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Revoke authentication token  
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authentication token attempted to revoke does not exist
     * @throws RepositoryServerException internal server error
     */
    public void revokeToken(@Nullable AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get authentication token by looking up token type and token value
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authentication token attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull AuthenticationToken getToken(@Nullable AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Delete expired authentication token
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @throws ValidationException
     * @throws ItemNotFoundException
     * @throws RepositoryServerException
     */
    public void deleteExpiredToken(@Nullable AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
