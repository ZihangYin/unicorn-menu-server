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
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.server.injector.TestRepositoryTableBinder;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserRepositoryImplTest {

    private static TestRepositoryTableBinder testRepositoryTableBinder;
    private static UserRepositoryImpl userRepositoryImpl;

    @BeforeClass
    public static void setUpRepository() throws Exception {
        testRepositoryTableBinder = new TestRepositoryTableBinder();
        userRepositoryImpl = new UserRepositoryImpl(testRepositoryTableBinder.getMockedDynamoUserProfileTable(),
                testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable(), testRepositoryTableBinder.getMockedDynamoMobilePhoneToPrincipalTable(),
                testRepositoryTableBinder.getMockedDynamoEmailAddressToPrincipalTable());
    }

    @After
    public void clearMockedRepository() {
        /*
         * Reset the mocking on this object so that the field can be safely re-used between tests.
         */
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoUserProfileTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoMobilePhoneToPrincipalTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoEmailAddressToPrincipalTable());
    }

    private void mockGetUserPrincipalFromUserNameHappyCase(Name userName, Long expectedUserPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doReturn(expectedUserPrincipal).when(mockedDynamoNameToPrincipalTable).getCurrentPrincipal(userName);
    }

    private void mockGetUserPrincipalFromMobilePhoneHappyCase(MobilePhone mobilePhone, Long expectedUserPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoMobilePhoneToPrincipalTable mockedDynamoMobilePhoneToPrincipalTable = testRepositoryTableBinder.getMockedDynamoMobilePhoneToPrincipalTable();
        Mockito.doReturn(expectedUserPrincipal).when(mockedDynamoMobilePhoneToPrincipalTable).getPrincipal(mobilePhone);
    }

    private void mockGetUserPrincipalFromEmailAddressHappyCase(EmailAddress emailAddress, Long expectedUserPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoEmailAddressToPrincipalTable mockedDynamoEmailAddressToPrincipalTable = testRepositoryTableBinder.getMockedDynamoEmailAddressToPrincipalTable();
        Mockito.doReturn(expectedUserPrincipal).when(mockedDynamoEmailAddressToPrincipalTable).getPrincipal(emailAddress);
    }

    private void mockGetUserPrincipalFromUserNameNoUserName(Name userName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoNameToPrincipalTable).getCurrentPrincipal(userName);
    }

    private void mockGetUserPrincipalFromMobilePhoneNoMobilePhone(MobilePhone mobilePhone) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoMobilePhoneToPrincipalTable mockedDynamoMobilePhoneToPrincipalTable = testRepositoryTableBinder.getMockedDynamoMobilePhoneToPrincipalTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoMobilePhoneToPrincipalTable).getPrincipal(mobilePhone);
    }

    private void mockGetUserPrincipalFromEmailAddressNoEmailAddress(EmailAddress emailAddress) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoEmailAddressToPrincipalTable mockedDynamoEmailAddressToPrincipalTable = testRepositoryTableBinder.getMockedDynamoEmailAddressToPrincipalTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoEmailAddressToPrincipalTable).getPrincipal(emailAddress);
    }
    
    private void mockCreateUserInUserProfileHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doReturn(0L).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserInUserProfileDuplicateUserPrincipalOnce() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoUserProfileTable mockedDynamoUserProfileTable = testRepositoryTableBinder.getMockedDynamoUserProfileTable();
        Mockito.doThrow(new DuplicateKeyException()).doReturn(0L).when(mockedDynamoUserProfileTable)
        .createUser(Mockito.anyLong(), Mockito.any(), Mockito.any(ByteBuffer.class), Mockito.any(ByteBuffer.class));
    }
    
    private void mockCreateUserInUserProfileDuplicateUserPrincipal() 
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
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doNothing().when(mockedDynamoNameToPrincipalTable).createNameForPrincipal(Mockito.any(), Mockito.anyLong());
    }
    
    private void mockCreateUserNameToIDDuplicateUserUserName() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doThrow(new DuplicateKeyException()).when(mockedDynamoNameToPrincipalTable).createNameForPrincipal(Mockito.any(), Mockito.anyLong());
    }
    
    private void mockCreateUserNameToIDServerError() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException  {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doThrow(new RepositoryServerException("internal_Server_error"))
        .when(mockedDynamoNameToPrincipalTable).createNameForPrincipal(Mockito.any(), Mockito.anyLong());
    }


    /*
     * Happy Case
     */
    @Test
    public void testGetUserPrincipalFromUserNameHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "user_name";
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserPrincipalFromUserNameHappyCase(Name.validateUserName(loginName), userPrincipal);
        assertEquals(userPrincipal, userRepositoryImpl.getPrincipalForLoginName(loginName));
    }

    @Test
    public void testGetUserPrincipalFromMobilePhoneHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "+19191234567";
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserPrincipalFromMobilePhoneHappyCase(new MobilePhone(loginName, null), userPrincipal);
        assertEquals(userPrincipal, userRepositoryImpl.getPrincipalForLoginName(loginName));
    }

    @Test
    public void testGetUserPrincipalFromEmailAddressHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "test@test.com";
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();

        mockGetUserPrincipalFromEmailAddressHappyCase(new EmailAddress(loginName), userPrincipal);
        assertEquals(userPrincipal, userRepositoryImpl.getPrincipalForLoginName(loginName));
    }
    
    @Test
    public void testCreateUserHappyCase() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileHappyCase();
        mockCreateUserNameToIDHappyCase();
        userRepositoryImpl.registerUser(userName, userDisplayName, password);
    }
    
    @Test
    public void testCreateUserDuplicateUserPrincipalOnceHappyCase() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserPrincipalOnce();
        mockCreateUserNameToIDHappyCase();
        userRepositoryImpl.registerUser(userName, userDisplayName, password);
    }

    /*
     * Bad Request
     */
    @Test
    public void testGetUserPrincipalFromUserNameDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "user_name";
        mockGetUserPrincipalFromUserNameNoUserName(Name.validateUserName(loginName));
        try {
            userRepositoryImpl.getPrincipalForLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserPrincipalFromUserNameDoesNotExist");
    }

    @Test
    public void testGetUserPrincipalFromMobilePhoneDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "+19191234567";
        mockGetUserPrincipalFromMobilePhoneNoMobilePhone(new MobilePhone(loginName, null));
        try {
            userRepositoryImpl.getPrincipalForLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserPrincipalFromMobilePhoneDoesNotExist");
    }

    @Test
    public void testGetUserPrincipalFromEmailAddressDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "test@test.com";
        mockGetUserPrincipalFromEmailAddressNoEmailAddress(new EmailAddress(loginName));
        try {
            userRepositoryImpl.getPrincipalForLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserPrincipalFromEmailAddressDoesNotExist");
    }
    
    @Test
    public void testCreateUserDuplicateUserName() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserPrincipalOnce();
        mockCreateUserNameToIDDuplicateUserUserName();
        try {
            userRepositoryImpl.registerUser(userName, userDisplayName, password);
        }catch (DuplicateKeyException error) {
            return;
        }
        fail("Failed while running testCreateUserDuplicateUserName");
    }
    
    /*
     * Internal Server Errors
     */
    @Test
    public void testCreateUserCreateUserInUserProfileDuplicateUserPrincipal() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileDuplicateUserPrincipal();
        try {
            userRepositoryImpl.registerUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            assertEquals(DuplicateKeyException.class, error.getCause().getClass());
            return;
        }
        fail("Failed while running testCreateUserDuplicateUserPrincipal");
    }
    
    @Test
    public void testCreateUserCreateUserInUserProfileServerError() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileServerError();
        try {
            userRepositoryImpl.registerUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            return;
        }
        fail("Failed while running testCreateUserServerError");
    }
    
    @Test
    public void testCreateUserCreateUserNameToIDServerError() 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        Name userName = Name.validateUserName("user_name");
        DisplayName userDisplayName = DisplayName.validateUserDisplayName("user_display_name");
        String password = "1a2b3c";
        
        mockCreateUserInUserProfileHappyCase();
        mockCreateUserNameToIDServerError();
        try {
            userRepositoryImpl.registerUser(userName, userDisplayName, password);
        } catch (RepositoryServerException error) {
            return;
        }
        fail("Failed while running testCreateUserServerError");
    }
    
}
