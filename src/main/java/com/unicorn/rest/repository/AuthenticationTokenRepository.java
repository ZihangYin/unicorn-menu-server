package com.unicorn.rest.repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;

public interface AuthenticationTokenRepository {

    /**
     * find authentication token from token type and token value
     * 
     * @param tokenType
     * @param token
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authentication token attempted to find does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull AuthenticationToken findToken(@Nullable AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * persist authentication token 
     * 
     * @param authenticationToken 
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if authentication token with same token type and token value already exists
     * @throws RepositoryServerException internal server error
     */
    public void persistToken(@Nullable AuthenticationToken authenticationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * revoke authentication token
     * 
     * @param tokenType
     * @param token
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authentication token attempted to revoke does not exist
     * @throws RepositoryServerException internal server error
     */
    public void revokeToken(@Nullable AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
