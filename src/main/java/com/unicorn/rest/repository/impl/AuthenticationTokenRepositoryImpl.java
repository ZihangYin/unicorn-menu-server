package com.unicorn.rest.repository.impl;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.repository.table.AuthenticationTokenTable;

public class AuthenticationTokenRepositoryImpl implements AuthenticationTokenRepository {

    private AuthenticationTokenTable authenticationTokenTable;

    @Inject
    public AuthenticationTokenRepositoryImpl(AuthenticationTokenTable authenticationTokenTable) {
        this.authenticationTokenTable = authenticationTokenTable;
    }
    
    @Override
    public AuthenticationToken findToken(AuthenticationTokenType tokenType, @Nullable String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        return authenticationTokenTable.getToken(tokenType, token);
    }

    @Override
    public void persistToken(AuthenticationToken authenticationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        authenticationTokenTable.persistToken(authenticationToken);
    }

    @Override
    public void revokeToken(AuthenticationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        authenticationTokenTable.revokeToken(tokenType, token);
    }

}
