package com.unicorn.rest.server.injector;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;

import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.table.EmailAddressToUserIdTable;
import com.unicorn.rest.repository.table.MobilePhoneToUserIdTable;
import com.unicorn.rest.repository.table.UserNameToUserIdTable;
import com.unicorn.rest.repository.table.UserProfileTable;

public class UserRepositoryFactory implements Factory<UserRepository> {

    private final UserRepository userRepository;

    @Inject 
    public UserRepositoryFactory(UserProfileTable userProfileTable, UserNameToUserIdTable userNameToUserIdTable, 
            MobilePhoneToUserIdTable mobilePhoneToUserIdTable, EmailAddressToUserIdTable emailAddressToUserIdTable){

        this.userRepository = new UserRepositoryImpl(userProfileTable,
                userNameToUserIdTable, mobilePhoneToUserIdTable, emailAddressToUserIdTable); 
    }

    @Override
    public UserRepository provide() {
        return userRepository;
    }

    @Override
    public void dispose(UserRepository instance) {
    }
}