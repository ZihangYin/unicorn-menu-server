package com.unicorn.rest.server.injector;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;

import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.impl.AuthenticationTokenRepositoryImpl;
import com.unicorn.rest.repository.table.AuthenticationTokenTable;

public class AuthenticationTokenRepositoryFactory implements Factory<AuthenticationTokenRepository> {
 
    private final AuthenticationTokenRepository authenticationTokenRepository;
    
    @Inject
    public AuthenticationTokenRepositoryFactory(AuthenticationTokenTable authenticationTokenTable) {
        this.authenticationTokenRepository = new AuthenticationTokenRepositoryImpl(authenticationTokenTable);
    }
    
    @Override
    public AuthenticationTokenRepository provide() {
        return authenticationTokenRepository;
    }

    @Override
    public void dispose(AuthenticationTokenRepository instance) {}
}
