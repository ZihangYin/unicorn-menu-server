package com.unicorn.rest.server.injector;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;

import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.table.EmailAddressToPrincipalTable;
import com.unicorn.rest.repository.table.MobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.table.NameToPrincipalTable;
import com.unicorn.rest.repository.table.UserProfileTable;

public class UserRepositoryFactory implements Factory<UserRepository> {

    private final UserRepository userRepository;

    @Inject 
    public UserRepositoryFactory(UserProfileTable userProfileTable, NameToPrincipalTable nameToPrincipalTable, 
            MobilePhoneToPrincipalTable mobilePhoneToPrincipalTable, EmailAddressToPrincipalTable emailAddressToPrincipalTable){

        this.userRepository = new UserRepositoryImpl(userProfileTable, nameToPrincipalTable, 
                mobilePhoneToPrincipalTable, emailAddressToPrincipalTable); 
    }

    @Override
    public UserRepository provide() {
        return userRepository;
    }

    @Override
    public void dispose(UserRepository instance) {
    }
}