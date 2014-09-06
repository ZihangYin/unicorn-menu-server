package com.unicorn.rest.server.injector;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.mockito.Mockito;

import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.AuthenticationTokenRepositoryImpl;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;

public class TestRepositoryBinder extends AbstractBinder {

    private AuthenticationTokenRepositoryImpl mockedTokenRepository = Mockito.mock(AuthenticationTokenRepositoryImpl.class);
    private UserRepositoryImpl mockedUserRepository = Mockito.mock(UserRepositoryImpl.class);;

    protected void configure() {
        bind(mockedTokenRepository).to(AuthenticationTokenRepository.class);
        bind(mockedUserRepository).to(UserRepository.class);
    }

    public AuthenticationTokenRepositoryImpl getMockedTokenRepository() {
        return mockedTokenRepository;
    }

    public UserRepositoryImpl getMockedUserRepository() {
        return mockedUserRepository;
    }
}
