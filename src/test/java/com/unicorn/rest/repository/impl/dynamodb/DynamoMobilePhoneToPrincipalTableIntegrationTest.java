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
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoMobilePhoneToPrincipalTableIntegrationTest {

    private static DynamoMobilePhoneToPrincipalTable mobilePhoneToPrincipalTable;

    @BeforeClass
    public static void setUpMobilePhoneToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
        mobilePhoneToPrincipalTable = new DynamoMobilePhoneToPrincipalTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        mobilePhoneToPrincipalTable.createTable();;
    }

    @Test
    public void testCreateMobilePhoneForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(mobilePhone, principal);
            Long persistedPrincipal = mobilePhoneToPrincipalTable.getPrincipal(mobilePhone);
            assertEquals(principal, persistedPrincipal);

        } finally {
            try {
                mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(mobilePhone, principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateMobilePhoneForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateMobilePhoneForPrincipalWithInvalidRequest");
    }

    @Test
    public void testCreateMobilePhoneForPrincipalWithExistedMobilePhone() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(mobilePhone, principal1);
            try {
                mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(mobilePhone, principal2);
            } catch(DuplicateKeyException error) {
                Long persistedPrincipal = mobilePhoneToPrincipalTable.getPrincipal(mobilePhone);
                assertEquals(principal1, persistedPrincipal);
                return;
            }
            fail("Failed while running testCreateMobilePhoneForPrincipalWithExistedMobilePhone");
        
        } finally {
            try {
                mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(mobilePhone, principal1);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateMobilePhoneForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(curMobilePhone, principal);
            mobilePhoneToPrincipalTable.updateMobilePhoneForPrincipal(curMobilePhone, newMobilePhone, principal);
            try {
                mobilePhoneToPrincipalTable.getPrincipal(curMobilePhone);
            } catch (ItemNotFoundException error) {
                Long persistedPrincipal = mobilePhoneToPrincipalTable.getPrincipal(newMobilePhone);
                assertEquals(principal, persistedPrincipal);
                MobilePhone persistedMobilePhone = mobilePhoneToPrincipalTable.getMobilePhone(principal, false);
                assertEquals(newMobilePhone, persistedMobilePhone);
                return;
            }
            fail("Failed while running testUpdateMobilePhoneForPrincipalHappyCase");
        
        } finally {
            try {
                mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(newMobilePhone, principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateMobilePhoneForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToPrincipalTable.updateMobilePhoneForPrincipal(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateMobilePhoneForPrincipalWithInvalidRequest");
    }

    @Test
    public void testUpdateMobilePhoneForPrincipalWithUnexpectedCurMobilePhone() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(curMobilePhone, principal);
            mobilePhoneToPrincipalTable.updateMobilePhoneForPrincipal(new MobilePhone("+13333333333", "US"), newMobilePhone, principal);
        } catch (ItemNotFoundException error) {
            assertEquals(principal, mobilePhoneToPrincipalTable.getPrincipal(curMobilePhone));
            return;
        
        } finally {
            try {
                mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(curMobilePhone, principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }

        fail("Failed while running testUpdateMobilePhoneForPrincipalWithUnexpectedCurMobilePhone");
    }

    @Test
    public void testUpdateMobilePhoneForPrincipalWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToPrincipalTable.updateMobilePhoneForPrincipal(curMobilePhone, newMobilePhone, principal);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateMobilePhoneForPrincipalWithNonExistedPrincipal");
    }

    @Test
    public void testUpdateMobilePhoneForPrincipalWithExistedMobilePhone() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(newMobilePhone, principal1);
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(curMobilePhone, principal2);

            mobilePhoneToPrincipalTable.updateMobilePhoneForPrincipal(curMobilePhone, newMobilePhone, principal2);
        } catch (DuplicateKeyException error) {
            assertEquals(principal1, mobilePhoneToPrincipalTable.getPrincipal(newMobilePhone));
            assertEquals(principal2, mobilePhoneToPrincipalTable.getPrincipal(curMobilePhone));
            return;

        } finally {
            mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(newMobilePhone, principal1);
            mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(curMobilePhone, principal2);
        }
        fail("Failed while running testUpdateMobilePhoneForPrincipalWithExistedMobilePhone");
    }

    // This is same as testCreateMobilePhoneForPrincipalHappyCase
    @Test
    public void testGetPrincipalHappyCase() {}

    @Test
    public void testGetPrincipalWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToPrincipalTable.getPrincipal(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithInvalidRequest");
    }

    @Test
    public void testGetPrincipalWithNonExistedMobilePhone() 
            throws ValidationException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        try {
            mobilePhoneToPrincipalTable.getPrincipal(mobilePhone);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithNonExistedMobilePhone");
    }

    @Test
    public void testGetMobilePhoneHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToPrincipalTable.createMobilePhoneForPrincipal(mobilePhone, principal);
            MobilePhone persistedMobilePhone = mobilePhoneToPrincipalTable.getMobilePhone(principal, false);
            assertEquals(mobilePhone, persistedMobilePhone);

        } finally {
            try {
                mobilePhoneToPrincipalTable.deleteMobilePhoneForPrincipal(mobilePhone, principal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetMobilePhoneWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToPrincipalTable.getMobilePhone(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetMobilePhoneWithInvalidRequest");
    }

    @Test
    public void testGetMobilePhoneWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToPrincipalTable.getMobilePhone(principal, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetMobilePhoneWithNonExistedPrincipal");
    }

    @AfterClass
    public static void tearDownMobilePhoneToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
//        mobilePhoneToPrincipalTable.deleteTable();
    }
}
