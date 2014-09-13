package com.unicorn.rest.server.injector;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.mockito.Mockito;

import com.unicorn.rest.repository.AuthorizationTokenRepository;
import com.unicorn.rest.repository.CustomerRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.AuthorizationTokenRepositoryImpl;
import com.unicorn.rest.repository.impl.CustomerRepositoryImpl;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;

public class TestRepositoryBinder extends AbstractBinder {

    private AuthorizationTokenRepositoryImpl mockedTokenRepository = Mockito.mock(AuthorizationTokenRepositoryImpl.class);
    private UserRepositoryImpl mockedUserRepository = Mockito.mock(UserRepositoryImpl.class);
    private CustomerRepositoryImpl mockedCustomerRepository = Mockito.mock(CustomerRepositoryImpl.class);

    protected void configure() {
        bind(mockedTokenRepository).to(AuthorizationTokenRepository.class);
        bind(mockedUserRepository).to(UserRepository.class);
        bind(mockedCustomerRepository).to(CustomerRepository.class);
    }

    public AuthorizationTokenRepositoryImpl getMockedTokenRepository() {
        return mockedTokenRepository;
    }

    public UserRepositoryImpl getMockedUserRepository() {
        return mockedUserRepository;
    }
    
    public CustomerRepositoryImpl getMockedCustomerRepository() {
        return mockedCustomerRepository;
    }
}
