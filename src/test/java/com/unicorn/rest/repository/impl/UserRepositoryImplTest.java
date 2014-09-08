package com.unicorn.rest.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserNameToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.server.injector.TestRepositoryTableBinder;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserRepositoryImplTest {

    private static TestRepositoryTableBinder testRepositoryTableBinder;
    private static UserRepositoryImpl userRepositoryImpl;

    @BeforeClass
    public static void setUpRepository() throws Exception {
        testRepositoryTableBinder = new TestRepositoryTableBinder();
        userRepositoryImpl = new UserRepositoryImpl(testRepositoryTableBinder.getMockedDynamoUserProfileTable(),
                testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable(), testRepositoryTableBinder.getMockedDynamoMobilePhoneToUserIdTable(),
                testRepositoryTableBinder.getMockedDynamoEmailAddressToUserIdTable());
    }

    @After
    public void clearMockedRepository() {
        /*
         * Reset the mocking on this object so that the field can be safely re-used between tests.
         */
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoUserProfileTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoMobilePhoneToUserIdTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoEmailAddressToUserIdTable());
    }

    private void mockGetUserIdFromUserNameHappyCase(UserName userName, Long expectedUserId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable();
        Mockito.doReturn(expectedUserId).when(mockedDynamoUserNameToUserIdTable).getCurrentUserId(userName);
    }

    private void mockGetUserIdFromMobilePhoneHappyCase(MobilePhone mobilePhone, Long expectedUserId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoMobilePhoneToUserIdTable mockedDynamoMobilePhoneToUserIdTable = testRepositoryTableBinder.getMockedDynamoMobilePhoneToUserIdTable();
        Mockito.doReturn(expectedUserId).when(mockedDynamoMobilePhoneToUserIdTable).getUserId(mobilePhone);
    }

    private void mockGetUserIdFromEmailAddressHappyCase(EmailAddress emailAddress, Long expectedUserId) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoEmailAddressToUserIdTable mockedDynamoEmailAddressToUserIdTable = testRepositoryTableBinder.getMockedDynamoEmailAddressToUserIdTable();
        Mockito.doReturn(expectedUserId).when(mockedDynamoEmailAddressToUserIdTable).getUserId(emailAddress);
    }

    private void mockGetUserIdFromUserNameNoUserName(UserName userName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoUserNameToUserIdTable).getCurrentUserId(userName);
    }

    private void mockGetUserIdFromMobilePhoneNoMobilePhone(MobilePhone mobilePhone) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoMobilePhoneToUserIdTable mockedDynamoMobilePhoneToUserIdTable = testRepositoryTableBinder.getMockedDynamoMobilePhoneToUserIdTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoMobilePhoneToUserIdTable).getUserId(mobilePhone);
    }

    private void mockGetUserIdFromEmailAddressNoEmailAddress(EmailAddress emailAddress) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoEmailAddressToUserIdTable mockedDynamoEmailAddressToUserIdTable = testRepositoryTableBinder.getMockedDynamoEmailAddressToUserIdTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoEmailAddressToUserIdTable).getUserId(emailAddress);
    }
    
    private void mockCreateUserInUserProfileHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doReturn(0L).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserInUserProfileDuplicateUserIdOnce() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doThrow(new DuplicateKeyException()).doReturn(0L).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserInUserProfileDuplicateUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doThrow(new DuplicateKeyException()).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserInUserProfileServerError() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doThrow(new RepositoryServerException("internal_Server_error")).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserNameToIDHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable();
        Mockito.doNothing().when(mockedDynamoUserNameToUserIdTable).createUserNameForUserId(Mockito.any(), Mockito.anyLong());
    }
    
    private void mockCreateUserNameToIDDuplicateUserUserName() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable();
        Mockito.doThrow(new DuplicateKeyException()).when(mockedDynamoUserNameToUserIdTable).createUserNameForUserId(Mockito.any(), Mockito.anyLong());
    }
    
    private void mockCreateUserNameToIDServerError() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = testRepositoryTableBinder.getMockedDynamoUserNameToUserIdTable();
        Mockito.doThrow(new RepositoryServerException("internal_Server_error")).when(mockedDynamoUserNameToUserIdTable).createUserNameForUserId(Mockito.any(), Mockito.anyLong());
    }


    /*
     * Happy Case
     */
    @Test
    public void testGetUserIdFromUserNameHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "user_name";
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserIdFromUserNameHappyCase(new UserName(loginName), userId);
        assertEquals(userId, userRepositoryImpl.getUserIdFromLoginName(loginName));
    }

    @Test
    public void testGetUserIdFromMobilePhoneHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "+19191234567";
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserIdFromMobilePhoneHappyCase(new MobilePhone(loginName, null), userId);
        assertEquals(userId, userRepositoryImpl.getUserIdFromLoginName(loginName));
    }

    @Test
    public void testGetUserIdFromEmailAddressHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "test@test.com";
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserIdFromEmailAddressHappyCase(new EmailAddress(loginName), userId);
        assertEquals(userId, userRepositoryImpl.getUserIdFromLoginName(loginName));
    }
    
    @Test
    public void testCreateUserHappyCase() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileHappyCase();
        mockCreateUserNameToIDHappyCase();
        userRepositoryImpl.createUser(userName, userDisplayName, password);
    }
    
    @Test
    public void testCreateUserDuplicateUserIdOnceHappyCase() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserIdOnce();
        mockCreateUserNameToIDHappyCase();
        userRepositoryImpl.createUser(userName, userDisplayName, password);
    }

    /*
     * Bad Request
     */
    @Test
    public void testGetUserIdFromUserNameDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "user_name";
        mockGetUserIdFromUserNameNoUserName(new UserName(loginName));
        try {
            userRepositoryImpl.getUserIdFromLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserIdFromUserNameDoesNotExist");
    }

    @Test
    public void testGetUserIdFromMobilePhoneDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "+19191234567";
        mockGetUserIdFromMobilePhoneNoMobilePhone(new MobilePhone(loginName, null));
        try {
            userRepositoryImpl.getUserIdFromLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserIdFromMobilePhoneDoesNotExist");
    }

    @Test
    public void testGetUserIdFromEmailAddressDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "test@test.com";
        mockGetUserIdFromEmailAddressNoEmailAddress(new EmailAddress(loginName));
        try {
            userRepositoryImpl.getUserIdFromLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserIdFromEmailAddressDoesNotExist");
    }
    
    @Test
    public void testCreateUserDuplicateUserName() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserIdOnce();
        mockCreateUserNameToIDDuplicateUserUserName();
        try {
            userRepositoryImpl.createUser(userName, userDisplayName, password);
        }catch (DuplicateKeyException error) {
            return;
        }
        fail("Failed while running testCreateUserDuplicateUserName");
    }
    
    /*
     * Internal Server Errors
     */
    @Test
    public void testCreateUserCreateUserInUserProfileDuplicateUserId() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserId();
        try {
            userRepositoryImpl.createUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            assertEquals(DuplicateKeyException.class, error.getCause().getClass());
            return;
        }
        fail("Failed while running testCreateUserDuplicateUserId");
    }
    
    @Test
    public void testCreateUserCreateUserInUserProfileServerError() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileServerError();
        try {
            userRepositoryImpl.createUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            return;
        }
        fail("Failed while running testCreateUserServerError");
    }
    
    @Test
    public void testCreateUserCreateUserNameToIDServerError() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserName userName = new UserName("user_name");
        UserDisplayName userDisplayName = new UserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileHappyCase();
        mockCreateUserNameToIDServerError();
        try {
            userRepositoryImpl.createUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            return;
        }
        fail("Failed while running testCreateUserServerError");
    }
    
}
