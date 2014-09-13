package com.unicorn.rest.server.injector;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthenticationTokenTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;
import com.unicorn.rest.repository.table.AuthenticationTokenTable;
import com.unicorn.rest.repository.table.EmailAddressToPrincipalTable;
import com.unicorn.rest.repository.table.MobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.table.NameToPrincipalTable;
import com.unicorn.rest.repository.table.UserProfileTable;

public class RepositoryBinder extends AbstractBinder {

    @Override
    protected void configure() {

        bind(new DynamoAuthenticationTokenTable()).to(AuthenticationTokenTable.class);
        bind(new DynamoNameToPrincipalTable()).to(NameToPrincipalTable.class);
        bind(new DynamoMobilePhoneToPrincipalTable()).to(MobilePhoneToPrincipalTable.class);
        bind(new DynamoEmailAddressToPrincipalTable()).to(EmailAddressToPrincipalTable.class);
        bind(new DynamoUserProfileTable()).to(UserProfileTable.class);
        
        bindFactory(AuthenticationTokenRepositoryFactory.class).to(AuthenticationTokenRepository.class).in(Singleton.class);
        bindFactory(UserRepositoryFactory.class).to(UserRepository.class).in(Singleton.class);
        
    }
}
