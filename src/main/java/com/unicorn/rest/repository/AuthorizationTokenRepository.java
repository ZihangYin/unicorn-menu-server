package com.unicorn.rest.repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;

public interface AuthorizationTokenRepository {

    /**
     * Find authorization token
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @param principal @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authorization token attempted to find does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull AuthorizationToken findToken(@Nullable AuthorizationTokenType tokenType, @Nullable String token, @Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Persist authorization token 
     * 
     * @param authorizationToken  @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if authorization token with same token type and token value already exists
     * @throws RepositoryServerException internal server error
     */
    public void persistToken(@Nullable AuthorizationToken authorizationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Revoke authorization token
     * 
     * @param tokenType @Nullable
     * @param token @Nullable
     * @param principal @Nullable
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if authorization token attempted to revoke does not exist
     * @throws RepositoryServerException internal server error
     */
    public void revokeToken(@Nullable AuthorizationTokenType tokenType, @Nullable String token, @Nullable Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
