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
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.repository.table.EmailAddressToUserIdTable;
import com.unicorn.rest.repository.table.MobilePhoneToUserIdTable;
import com.unicorn.rest.repository.table.UserNameToUserIdTable;
import com.unicorn.rest.repository.table.UserProfileTable;
import com.unicorn.rest.utils.PasswordAuthenticationHelper;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger LOG = LogManager.getLogger(UserRepositoryImpl.class);
    
    private UserProfileTable userProfileTable;
    private UserNameToUserIdTable userNameToUserIdTable;
    private MobilePhoneToUserIdTable mobilePhoneToUserIdTable;
    private EmailAddressToUserIdTable emailAddressToUserIdTable;

    @Inject
    public UserRepositoryImpl(UserProfileTable userProfileTable, UserNameToUserIdTable userNameToUserIdTable, MobilePhoneToUserIdTable mobilePhoneToUserIdTable, 
            EmailAddressToUserIdTable emailAddressToUserIdTable) {
        this.userProfileTable = userProfileTable;
        this.userNameToUserIdTable = userNameToUserIdTable;
        this.mobilePhoneToUserIdTable = mobilePhoneToUserIdTable;
        this.emailAddressToUserIdTable = emailAddressToUserIdTable;
    }

    @Override
    public Long getUserIdFromLoginName(String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (StringUtils.isBlank(loginName)) {
            throw new ValidationException("Expecting non-null request paramter for getUserIdFromLoginName, but received: loginName=null");
        }
        /**
         * TODO: user_name should in future contain at least one letter so that
         * we can differentiate user_name from mobile_phone based on that
         */
        if (loginName.startsWith("+")) {
            MobilePhone mobilePhone = new MobilePhone(loginName, null);
            return mobilePhoneToUserIdTable.getUserId(mobilePhone);
        } else if (loginName.contains("@")){
            EmailAddress emailAddress = new EmailAddress(loginName);
            return emailAddressToUserIdTable.getUserId(emailAddress);
        } else {
            UserName userName = new UserName(loginName);
            return userNameToUserIdTable.getCurrentUserId(userName);
        }
    }

    @Override
    public UserAuthorizationInfo getUserAuthorizationInfo(Long userId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        return userProfileTable.getUserAuthorizationInfo(userId);
    }

    @Override
    public Long createUser(UserName userName, UserDisplayName userDisplayName,
            String password) throws ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        ByteBuffer salt = PasswordAuthenticationHelper.generateRandomSalt();
        ByteBuffer hasedPassword = PasswordAuthenticationHelper.generateHashedPassWithSalt(password, salt);
        
        try {
            userProfileTable.createUser(userId, userDisplayName, hasedPassword, salt);
        } catch(DuplicateKeyException duplicateKeyOnce) {
            /**
             * Here we try one more time to create user record only if we get back DuplicateKeyException for user_id. 
             * If we still fail after that, throw exception and log an error.
             * 
             * TODO: monitor how often this happens
             */
            LOG.warn("Failed to create user for user {} with user_id {} due to duplicate user_id already exists.", userName, userId);
            userId = SimpleFlakeKeyGenerator.generateKey();
            try {
                userProfileTable.createUser(userId, userDisplayName, hasedPassword, salt);
            
            } catch(DuplicateKeyException duplicateKeyAgain) {
                LOG.error("Failed to create user for user {} with user_id {} for the second time due to duplicate user_id already exists.", 
                        userName, userId);
                throw new RepositoryServerException(duplicateKeyAgain);
            }
        }
        /**
         * TODO: If the following step failed later for whatever reason, we will
         * have non-associated user_id record in the USER_PROFILE table. We should 
         * add a sweeper to remove those records.
         */
        userNameToUserIdTable.createUserNameForUserId(userName, userId);
        return userId;
    }
}
