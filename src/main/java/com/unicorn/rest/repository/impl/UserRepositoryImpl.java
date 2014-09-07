package com.unicorn.rest.repository.impl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.repository.table.EmailAddressToUserIdTable;
import com.unicorn.rest.repository.table.MobilePhoneToUserIdTable;
import com.unicorn.rest.repository.table.UserNameToUserIdTable;
import com.unicorn.rest.repository.table.UserProfileTable;
import com.unicorn.rest.utils.PasswordAuthenticationHelper;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserRepositoryImpl implements UserRepository {

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
            throw new ValidationException("Expecting non-null request paramter for getUserIdFromLoginName, but received: loginName=null.");
        }
        
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
    public Long createUser(UserName userName, String userDisplayName,
            String password) throws ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        ByteBuffer salt = PasswordAuthenticationHelper.generateRandomSalt();
        ByteBuffer hasedPassword = PasswordAuthenticationHelper.generateHashedPassWithSalt(password, salt);
        
        userProfileTable.createUser(userId, userDisplayName, hasedPassword, salt);
        userNameToUserIdTable.createUserNameForUserId(userName, userId);
        return userId;
    }

}
