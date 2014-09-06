package com.unicorn.rest.server.injector;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthenticationTokenTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserNameToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;
import com.unicorn.rest.repository.table.AuthenticationTokenTable;
import com.unicorn.rest.repository.table.EmailAddressToUserIdTable;
import com.unicorn.rest.repository.table.MobilePhoneToUserIdTable;
import com.unicorn.rest.repository.table.UserNameToUserIdTable;
import com.unicorn.rest.repository.table.UserProfileTable;

public class RepositoryBinder extends AbstractBinder {

    @Override
    protected void configure() {

        bind(new DynamoAuthenticationTokenTable()).to(AuthenticationTokenTable.class);
        bind(new DynamoUserNameToUserIdTable()).to(UserNameToUserIdTable.class);
        bind(new DynamoMobilePhoneToUserIdTable()).to(MobilePhoneToUserIdTable.class);
        bind(new DynamoEmailAddressToUserIdTable()).to(EmailAddressToUserIdTable.class);
        bind(new DynamoUserProfileTable()).to(UserProfileTable.class);
        
        bindFactory(AuthenticationTokenRepositoryFactory.class).to(AuthenticationTokenRepository.class).in(Singleton.class);
        bindFactory(UserRepositoryFactory.class).to(UserRepository.class).in(Singleton.class);
        
    }
}
