package com.unicorn.rest.server.injector;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;

import com.unicorn.rest.repository.AuthorizationTokenRepository;
import com.unicorn.rest.repository.impl.AuthorizationTokenRepositoryImpl;
import com.unicorn.rest.repository.table.AuthorizationTokenTable;

public class AuthorizationTokenRepositoryFactory implements Factory<AuthorizationTokenRepository> {
 
    private final AuthorizationTokenRepository authorizationTokenRepository;
    
    @Inject
    public AuthorizationTokenRepositoryFactory(AuthorizationTokenTable authorizationTokenTable) {
        this.authorizationTokenRepository = new AuthorizationTokenRepositoryImpl(authorizationTokenTable);
    }
    
    @Override
    public AuthorizationTokenRepository provide() {
        return authorizationTokenRepository;
    }

    @Override
    public void dispose(AuthorizationTokenRepository instance) {}
}
