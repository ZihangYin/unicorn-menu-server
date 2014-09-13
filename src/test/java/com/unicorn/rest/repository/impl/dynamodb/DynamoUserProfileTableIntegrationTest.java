package com.unicorn.rest.repository.impl.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.utils.AuthenticationSecretUtils;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoUserProfileTableIntegrationTest {

    private static DynamoUserProfileTable userProfileTable;

    @BeforeClass
    public static void setUpUserProfileTable() throws RepositoryClientException, RepositoryServerException {
        userProfileTable = new DynamoUserProfileTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        userProfileTable.createTable();
    }

    @Test
    public void testCreateUserHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        UserDisplayName userDisplayName = new UserDisplayName("userDisplayName");
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt);

        try {
            userProfileTable.createUser(userId, userDisplayName, password, salt);
            PrincipalAuthenticationInfo userAuthorizationInfo = userProfileTable.getUserAuthorizationInfo(userId);
            assertEquals(userId, userAuthorizationInfo.getPrincipal());
            assertEquals(password, userAuthorizationInfo.getPassword());
            assertEquals(salt, userAuthorizationInfo.getSalt());

        } finally {
            try {
                userProfileTable.deleteUser(userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateUserWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            userProfileTable.createUser(null, null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateUserWithInvalidRequest");
    }

    @Test
    public void testCreateUserWithExistedUserId() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        UserDisplayName userDisplayName1 = new UserDisplayName("userDisplayNameOne");
        UserDisplayName userDisplayName2 = new UserDisplayName("userDisplayNameTwo");
        ByteBuffer salt1 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer salt2 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password1 = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt1);
        ByteBuffer password2 = AuthenticationSecretUtils.generateHashedSecretWithSalt("3c2b1a", salt2);
        try {
            userProfileTable.createUser(userId, userDisplayName1, password1, salt1);
            try {
                userProfileTable.createUser(userId, userDisplayName2, password2, salt2);
            } catch(DuplicateKeyException error) {
                PrincipalAuthenticationInfo userAuthorizationInfo = userProfileTable.getUserAuthorizationInfo(userId);
                assertEquals(userId, userAuthorizationInfo.getPrincipal());
                assertEquals(password1, userAuthorizationInfo.getPassword());
                assertEquals(salt1, userAuthorizationInfo.getSalt());
                return;
            }
            fail("Failed while running testCreateUserWithExistedUserId");

        } finally {
            try {
                userProfileTable.deleteUser(userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    // This is same as testCreateUserHappyCase
    @Test
    public void testGetUserAuthorizationInfoHappyCase() {}

    @Test
    public void testGetUserAuthorizationInfoWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            userProfileTable.getUserAuthorizationInfo(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserAuthorizationInfoHappyCase");
    }

    @Test
    public void testGetUserAuthorizationInfoWithNonExistedUser() 
            throws ValidationException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userProfileTable.getUserAuthorizationInfo(userId);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetUserAuthorizationInfoWithNonExistedUser");
    }

    @AfterClass
    public static void tearDownUserProfileTable() throws RepositoryClientException, RepositoryServerException {
//        userProfileTable.deleteTable();
    }
}
