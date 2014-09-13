package com.unicorn.rest.repository.impl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.repository.model.PrincipalAuthorizationInfo;
import com.unicorn.rest.repository.table.EmailAddressToPrincipalTable;
import com.unicorn.rest.repository.table.MobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.table.NameToPrincipalTable;
import com.unicorn.rest.repository.table.UserProfileTable;
import com.unicorn.rest.utils.AuthenticationSecretUtils;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOG = LogManager.getLogger(UserRepositoryImpl.class);
    
    private UserProfileTable userProfileTable;
    private NameToPrincipalTable nameToPrincipalTable;
    private MobilePhoneToPrincipalTable mobilePhoneToPrincipalTable;
    private EmailAddressToPrincipalTable emailAddressToPrincipalTable;

    @Inject
    public UserRepositoryImpl(UserProfileTable userProfileTable, NameToPrincipalTable nameToPrincipalTable, 
            MobilePhoneToPrincipalTable mobilePhoneToPrincipalTable, EmailAddressToPrincipalTable emailAddressToPrincipalTable) {
        this.userProfileTable = userProfileTable;
        this.nameToPrincipalTable = nameToPrincipalTable;
        this.mobilePhoneToPrincipalTable = mobilePhoneToPrincipalTable;
        this.emailAddressToPrincipalTable = emailAddressToPrincipalTable;
    }

    @Override
    public Long getPrincipalForLoginName(String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (StringUtils.isBlank(loginName)) {
            throw new ValidationException("Expecting non-null request paramter for getPrincipalForLoginName, but received: loginName=null");
        }
        /**
         * TODO: user_name should in future contain at least one letter so that
         * we can differentiate user_name from mobile_phone based on that
         */
        if (loginName.startsWith("+")) {
            MobilePhone mobilePhone = new MobilePhone(loginName, null);
            return mobilePhoneToPrincipalTable.getPrincipal(mobilePhone);
        } else if (loginName.contains("@")){
            EmailAddress emailAddress = new EmailAddress(loginName);
            return emailAddressToPrincipalTable.getPrincipal(emailAddress);
        } else {
            Name userName = new Name(loginName);
            return nameToPrincipalTable.getCurrentPrincipal(userName);
        }
    }

    @Override
    public PrincipalAuthorizationInfo getAuthorizationInfoForPrincipal(Long userPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        return userProfileTable.getUserAuthorizationInfo(userPrincipal);
    }

    @Override
    public Long registerUser(Name userName, DisplayName userDisplayName,
            String password) throws ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer hasedPassword = AuthenticationSecretUtils.generateHashedSecretWithSalt(password, salt);
        
        try {
            userProfileTable.createUser(userPrincipal, userDisplayName, hasedPassword, salt);
            
        } catch(DuplicateKeyException duplicateKeyOnce) {
            /**
             * Here we try one more time to create user record only if we get back DuplicateKeyException for user_principal. 
             * If we still fail after that, throw exception and log an error.
             * 
             * TODO: monitor how often this happens
             */
            LOG.warn("Failed to create user for user {} with user_principal {} due to duplicate user_principal already exists.", userName, userPrincipal);
            userPrincipal = SimpleFlakeKeyGenerator.generateKey();
            try {
                userProfileTable.createUser(userPrincipal, userDisplayName, hasedPassword, salt);
            
            } catch(DuplicateKeyException duplicateKeyAgain) {
                LOG.error("Failed to create user for user {} with user_principal {} for the second time due to duplicate user_principal already exists.", 
                        userName, userPrincipal);
                throw new RepositoryServerException(duplicateKeyAgain);
            }
        }
        /**
         * TODO: If the following step failed later for whatever reason, we will
         * have non-associated user_principal record in the USER_PROFILE table. We should 
         * add a sweeper to remove those records.
         */
        nameToPrincipalTable.createNameForPrincipal(userName, userPrincipal);
        return userPrincipal;
    }
}
