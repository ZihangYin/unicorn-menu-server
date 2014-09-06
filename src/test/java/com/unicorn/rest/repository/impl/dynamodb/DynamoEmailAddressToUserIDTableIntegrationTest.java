package com.unicorn.rest.repository.impl.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoEmailAddressToUserIDTableIntegrationTest {

    private static DynamoEmailAddressToUserIdTable emailAddressToUserIdTable;

    @BeforeClass
    public static void setUpEmailAddressToUserIDTable() throws RepositoryClientException, RepositoryServerException {
        emailAddressToUserIdTable = new DynamoEmailAddressToUserIdTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        emailAddressToUserIdTable.createTable();
    }

    @Test
    public void testCreateEmailAddressForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(emailAddress, userId);
            Long persistedUserId = emailAddressToUserIdTable.getUserId(emailAddress);
            assertEquals(userId, persistedUserId);

        } finally {
            try {
                emailAddressToUserIdTable.deleteEmailAddressForUserId(emailAddress.getEmailAddress(), userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateEmailAddressForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateEmailAddressForUserIdWithInvalidRequest");
    }

    @Test
    public void testCreateEmailAddressForUserIdWithExistedEmailAddress() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(emailAddress, userId1);
            try {
                emailAddressToUserIdTable.createEmailAddressForUserId(emailAddress, userId2);
            } catch(DuplicateKeyException error) {
                Long persistedUserId = emailAddressToUserIdTable.getUserId(emailAddress);
                assertEquals(userId1, persistedUserId);
                return;
            }
            fail("Failed while running testCreateEmailAddressForUserIdWithExistedEmailAddress");
        
        } finally {
            try {
                emailAddressToUserIdTable.deleteEmailAddressForUserId(emailAddress.getEmailAddress(), userId1);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateEmailAddressForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(curEmailAddress, userId);
            emailAddressToUserIdTable.updateEmailAddressForUserId(curEmailAddress, newEmailAddress, userId);
            try {
                emailAddressToUserIdTable.getUserId(curEmailAddress);
            } catch (ItemNotFoundException error) {
                Long persistedUserId = emailAddressToUserIdTable.getUserId(newEmailAddress);
                assertEquals(userId, persistedUserId);
                String persistedEmailAddress = emailAddressToUserIdTable.getEmailAddress(userId, false);
                assertEquals(newEmailAddress.getEmailAddress(), persistedEmailAddress);
                return;
            }
            fail("Failed while running testUpdateEmailAddressForUserIdHappyCase");
        
        } finally {
            try {
                emailAddressToUserIdTable.deleteEmailAddressForUserId(newEmailAddress.getEmailAddress(), userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateEmailAddressForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToUserIdTable.updateEmailAddressForUserId(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateEmailAddressForUserIdWithInvalidRequest");
    }

    @Test
    public void testUpdateEmailAddressForUserIdWithUnexpectedCurEmailAddress() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(curEmailAddress, userId);
            emailAddressToUserIdTable.updateEmailAddressForUserId(new EmailAddress("wrong_cur_test@gmail.com"), newEmailAddress, userId);
        } catch (ItemNotFoundException error) {
            assertEquals(userId, emailAddressToUserIdTable.getUserId(curEmailAddress));
            return;
        
        } finally {
            try {
                emailAddressToUserIdTable.deleteEmailAddressForUserId(curEmailAddress.getEmailAddress(), userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
        
        fail("Failed while running testUpdateEmailAddressForUserIdWithUnexpectedCurEmailAddress");
    }

    @Test
    public void testUpdateEmailAddressForUserIdWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToUserIdTable.updateEmailAddressForUserId(curEmailAddress, newEmailAddress, userId);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateEmailAddressForUserIdWithNonExistedUserId");
    }
    
    @Test
    public void testUpdateEmailAddressForUserIdWithExistedEmailAddress() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(newEmailAddress, userId1);
            emailAddressToUserIdTable.createEmailAddressForUserId(curEmailAddress, userId2);
            
            emailAddressToUserIdTable.updateEmailAddressForUserId(curEmailAddress, newEmailAddress, userId2);
        } catch (DuplicateKeyException error) {
            assertEquals(userId1, emailAddressToUserIdTable.getUserId(newEmailAddress));
            assertEquals(userId2, emailAddressToUserIdTable.getUserId(curEmailAddress));
            return;
            
        } finally {
            emailAddressToUserIdTable.deleteEmailAddressForUserId(newEmailAddress.getEmailAddress(), userId1);
            emailAddressToUserIdTable.deleteEmailAddressForUserId(curEmailAddress.getEmailAddress(), userId2);
        }
        fail("Failed while running testUpdateEmailAddressForUserIdWithExistedEmailAddress");
    }
    
    // This is same as testCreateEmailAddressForUserIdHappyCase
    @Test
    public void testGetUserIdHappyCase() {}
    
    @Test
    public void testGetUserIdWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToUserIdTable.getUserId(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithInvalidRequest");
    }
    
    @Test
    public void testGetUserIdWithNonExistedEmailAddress() 
            throws ValidationException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        try {
            emailAddressToUserIdTable.getUserId(emailAddress);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithNonExistedEmailAddress");
    }
    
    @Test
    public void testGetEmailAddressHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToUserIdTable.createEmailAddressForUserId(emailAddress, userId);
            String persistedEmailAddress = emailAddressToUserIdTable.getEmailAddress(userId, false);
            assertEquals(emailAddress.getEmailAddress(), persistedEmailAddress);

        } finally {
            try {
                emailAddressToUserIdTable.deleteEmailAddressForUserId(emailAddress.getEmailAddress(), userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testGetEmailAddressWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToUserIdTable.getEmailAddress(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetEmailAddressWithInvalidRequest");
    }
    
    @Test
    public void testGetEmailAddressWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToUserIdTable.getEmailAddress(userId, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetEmailAddressWithNonExistedUserId");
    }
    
    @AfterClass
    public static void tearDownEmailAddressToUserIDTable() throws RepositoryClientException, RepositoryServerException {
//        emailAddressToUserIdTable.deleteTable();
    }
}
