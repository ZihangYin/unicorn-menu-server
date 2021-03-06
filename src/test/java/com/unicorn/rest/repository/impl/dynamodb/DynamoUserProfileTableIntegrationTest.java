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
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
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
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("userDisplayName");
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt);

        try {
            userProfileTable.createUser(userPrincipal, userDisplayName, password, salt);
            PrincipalAuthenticationInfo userAuthenticationInfo = userProfileTable.getUserAuthenticationInfo(userPrincipal);
            assertEquals(userPrincipal, userAuthenticationInfo.getPrincipal());
            assertEquals(password, userAuthenticationInfo.getPassword());
            assertEquals(salt, userAuthenticationInfo.getSalt());

        } finally {
            try {
                userProfileTable.deleteUser(userPrincipal);
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
    public void testCreateUserWithExistedUserPrincipal() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        DisplayName userDisplayName1 = DisplayName.validateUserDisplayName("userDisplayNameOne");
        DisplayName userDisplayName2 = DisplayName.validateUserDisplayName("userDisplayNameTwo");
        ByteBuffer salt1 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer salt2 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password1 = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt1);
        ByteBuffer password2 = AuthenticationSecretUtils.generateHashedSecretWithSalt("3c2b1a", salt2);
        try {
            userProfileTable.createUser(userPrincipal, userDisplayName1, password1, salt1);
            try {
                userProfileTable.createUser(userPrincipal, userDisplayName2, password2, salt2);
            } catch(DuplicateKeyException error) {
                PrincipalAuthenticationInfo userAuthenticationInfo = userProfileTable.getUserAuthenticationInfo(userPrincipal);
                assertEquals(userPrincipal, userAuthenticationInfo.getPrincipal());
                assertEquals(password1, userAuthenticationInfo.getPassword());
                assertEquals(salt1, userAuthenticationInfo.getSalt());
                return;
            }
            fail("Failed while running testCreateUserWithExistedUserPrincipal");

        } finally {
            try {
                userProfileTable.deleteUser(userPrincipal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    // This is same as testCreateUserHappyCase
    @Test
    public void testGetUserAuthenticationInfoHappyCase() {}

    @Test
    public void testGetUserAuthenticationInfoWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            userProfileTable.getUserAuthenticationInfo(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserAuthenticationInfoHappyCase");
    }

    @Test
    public void testGetUserAuthenticationInfoWithNonExistedUser() 
            throws ValidationException, RepositoryServerException {
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        try {
            userProfileTable.getUserAuthenticationInfo(userPrincipal);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetUserAuthenticationInfoWithNonExistedUser");
    }

    @AfterClass
    public static void tearDownUserProfileTable() throws RepositoryClientException, RepositoryServerException {
//        userProfileTable.deleteTable();
    }
}
