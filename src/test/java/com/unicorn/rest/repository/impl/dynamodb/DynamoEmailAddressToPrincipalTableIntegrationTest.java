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
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToPrincipalTable;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoEmailAddressToPrincipalTableIntegrationTest {

    private static DynamoEmailAddressToPrincipalTable emailAddressToPrincipalTable;

    @BeforeClass
    public static void setUpEmailAddressToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
        emailAddressToPrincipalTable = new DynamoEmailAddressToPrincipalTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        emailAddressToPrincipalTable.createTable();
    }

    @Test
    public void testCreateEmailAddressForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(emailAddress, principal);
            Long persistedPrincipal = emailAddressToPrincipalTable.getPrincipal(emailAddress);
            assertEquals(principal, persistedPrincipal);

        } finally {
            try {
                emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(emailAddress.getEmailAddress(), principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateEmailAddressForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateEmailAddressForPrincipalWithInvalidRequest");
    }

    @Test
    public void testCreateEmailAddressForPrincipalWithExistedEmailAddress() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(emailAddress, principal1);
            try {
                emailAddressToPrincipalTable.createEmailAddressForPrincipal(emailAddress, principal2);
            } catch(DuplicateKeyException error) {
                Long persistedPrincipal = emailAddressToPrincipalTable.getPrincipal(emailAddress);
                assertEquals(principal1, persistedPrincipal);
                return;
            }
            fail("Failed while running testCreateEmailAddressForPrincipalWithExistedEmailAddress");
        
        } finally {
            try {
                emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(emailAddress.getEmailAddress(), principal1);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateEmailAddressForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(curEmailAddress, principal);
            emailAddressToPrincipalTable.updateEmailAddressForPrincipal(curEmailAddress, newEmailAddress, principal);
            try {
                emailAddressToPrincipalTable.getPrincipal(curEmailAddress);
            } catch (ItemNotFoundException error) {
                Long persistedPrincipal = emailAddressToPrincipalTable.getPrincipal(newEmailAddress);
                assertEquals(principal, persistedPrincipal);
                String persistedEmailAddress = emailAddressToPrincipalTable.getEmailAddress(principal, false);
                assertEquals(newEmailAddress.getEmailAddress(), persistedEmailAddress);
                return;
            }
            fail("Failed while running testUpdateEmailAddressForPrincipalHappyCase");
        
        } finally {
            try {
                emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(newEmailAddress.getEmailAddress(), principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateEmailAddressForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToPrincipalTable.updateEmailAddressForPrincipal(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateEmailAddressForPrincipalWithInvalidRequest");
    }

    @Test
    public void testUpdateEmailAddressForPrincipalWithUnexpectedCurEmailAddress() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(curEmailAddress, principal);
            emailAddressToPrincipalTable.updateEmailAddressForPrincipal(new EmailAddress("wrong_cur_test@gmail.com"), newEmailAddress, principal);
        } catch (ItemNotFoundException error) {
            assertEquals(principal, emailAddressToPrincipalTable.getPrincipal(curEmailAddress));
            return;
        
        } finally {
            try {
                emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(curEmailAddress.getEmailAddress(), principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
        
        fail("Failed while running testUpdateEmailAddressForPrincipalWithUnexpectedCurEmailAddress");
    }

    @Test
    public void testUpdateEmailAddressForPrincipalWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToPrincipalTable.updateEmailAddressForPrincipal(curEmailAddress, newEmailAddress, principal);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateEmailAddressForPrincipalWithNonExistedPrincipal");
    }
    
    @Test
    public void testUpdateEmailAddressForPrincipalWithExistedEmailAddress() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        EmailAddress curEmailAddress = new EmailAddress("cur_test@gmail.com");
        EmailAddress newEmailAddress = new EmailAddress("new_test@gmail.com");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(newEmailAddress, principal1);
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(curEmailAddress, principal2);
            
            emailAddressToPrincipalTable.updateEmailAddressForPrincipal(curEmailAddress, newEmailAddress, principal2);
        } catch (DuplicateKeyException error) {
            assertEquals(principal1, emailAddressToPrincipalTable.getPrincipal(newEmailAddress));
            assertEquals(principal2, emailAddressToPrincipalTable.getPrincipal(curEmailAddress));
            return;
            
        } finally {
            emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(newEmailAddress.getEmailAddress(), principal1);
            emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(curEmailAddress.getEmailAddress(), principal2);
        }
        fail("Failed while running testUpdateEmailAddressForPrincipalWithExistedEmailAddress");
    }
    
    // This is same as testCreateEmailAddressForPrincipalHappyCase
    @Test
    public void testGetPrincipalHappyCase() {}
    
    @Test
    public void testGetPrincipalWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToPrincipalTable.getPrincipal(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithInvalidRequest");
    }
    
    @Test
    public void testGetPrincipalWithNonExistedEmailAddress() 
            throws ValidationException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        try {
            emailAddressToPrincipalTable.getPrincipal(emailAddress);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithNonExistedEmailAddress");
    }
    
    @Test
    public void testGetEmailAddressHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        EmailAddress emailAddress = new EmailAddress("test@gmail.com");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToPrincipalTable.createEmailAddressForPrincipal(emailAddress, principal);
            String persistedEmailAddress = emailAddressToPrincipalTable.getEmailAddress(principal, false);
            assertEquals(emailAddress.getEmailAddress(), persistedEmailAddress);

        } finally {
            try {
                emailAddressToPrincipalTable.deleteEmailAddressForPrincipal(emailAddress.getEmailAddress(), principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testGetEmailAddressWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            emailAddressToPrincipalTable.getEmailAddress(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetEmailAddressWithInvalidRequest");
    }
    
    @Test
    public void testGetEmailAddressWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            emailAddressToPrincipalTable.getEmailAddress(principal, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetEmailAddressWithNonExistedPrincipal");
    }
    
    @AfterClass
    public static void tearDownEmailAddressToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
//        emailAddressToPrincipalTable.deleteTable();
    }
}
