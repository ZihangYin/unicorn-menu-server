package com.unicorn.rest.repository.impl;

import javax.inject.Inject;

import com.unicorn.rest.repository.AuthorizationTokenRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;
import com.unicorn.rest.repository.table.AuthorizationTokenTable;

public class AuthorizationTokenRepositoryImpl implements AuthorizationTokenRepository {

    private AuthorizationTokenTable authorizationTokenTable;

    @Inject
    public AuthorizationTokenRepositoryImpl(AuthorizationTokenTable authorizationTokenTable) {
        this.authorizationTokenTable = authorizationTokenTable;
    }
    
    @Override
    public AuthorizationToken findToken(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        return authorizationTokenTable.getTokenForPrincipal(tokenType, token, principal);
    }

    @Override
    public void persistToken(AuthorizationToken authorizationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        authorizationTokenTable.persistToken(authorizationToken);
    }

    @Override
    public void revokeToken(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        authorizationTokenTable.revokeTokenForPrincipal(tokenType, token, principal);
    }

}
