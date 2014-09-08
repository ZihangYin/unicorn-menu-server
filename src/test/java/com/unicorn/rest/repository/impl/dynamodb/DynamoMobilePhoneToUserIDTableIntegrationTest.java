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
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToUserIdTable;
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoMobilePhoneToUserIDTableIntegrationTest {

    private static DynamoMobilePhoneToUserIdTable mobilePhoneToUserIdTable;

    @BeforeClass
    public static void setUpMobilePhoneToUserIdTable() throws RepositoryClientException, RepositoryServerException {
        mobilePhoneToUserIdTable = new DynamoMobilePhoneToUserIdTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        mobilePhoneToUserIdTable.createTable();;
    }

    @Test
    public void testCreateMobilePhoneForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(mobilePhone, userId);
            Long persistedUserId = mobilePhoneToUserIdTable.getUserId(mobilePhone);
            assertEquals(userId, persistedUserId);

        } finally {
            try {
                mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(mobilePhone, userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateMobilePhoneForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateMobilePhoneForUserIdWithInvalidRequest");
    }

    @Test
    public void testCreateMobilePhoneForUserIdWithExistedMobilePhone() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(mobilePhone, userId1);
            try {
                mobilePhoneToUserIdTable.createMobilePhoneForUserId(mobilePhone, userId2);
            } catch(DuplicateKeyException error) {
                Long persistedUserId = mobilePhoneToUserIdTable.getUserId(mobilePhone);
                assertEquals(userId1, persistedUserId);
                return;
            }
            fail("Failed while running testCreateMobilePhoneForUserIdWithExistedMobilePhone");
        
        } finally {
            try {
                mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(mobilePhone, userId1);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateMobilePhoneForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(curMobilePhone, userId);
            mobilePhoneToUserIdTable.updateMobilePhoneForUserId(curMobilePhone, newMobilePhone, userId);
            try {
                mobilePhoneToUserIdTable.getUserId(curMobilePhone);
            } catch (ItemNotFoundException error) {
                Long persistedUserId = mobilePhoneToUserIdTable.getUserId(newMobilePhone);
                assertEquals(userId, persistedUserId);
                MobilePhone persistedMobilePhone = mobilePhoneToUserIdTable.getMobilePhone(userId, false);
                assertEquals(newMobilePhone, persistedMobilePhone);
                return;
            }
            fail("Failed while running testUpdateMobilePhoneForUserIdHappyCase");
        
        } finally {
            try {
                mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(newMobilePhone, userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateMobilePhoneForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToUserIdTable.updateMobilePhoneForUserId(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateMobilePhoneForUserIdWithInvalidRequest");
    }

    @Test
    public void testUpdateMobilePhoneForUserIdWithUnexpectedCurMobilePhone() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(curMobilePhone, userId);
            mobilePhoneToUserIdTable.updateMobilePhoneForUserId(new MobilePhone("+13333333333", "US"), newMobilePhone, userId);
        } catch (ItemNotFoundException error) {
            assertEquals(userId, mobilePhoneToUserIdTable.getUserId(curMobilePhone));
            return;
        
        } finally {
            try {
                mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(curMobilePhone, userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }

        fail("Failed while running testUpdateMobilePhoneForUserIdWithUnexpectedCurMobilePhone");
    }

    @Test
    public void testUpdateMobilePhoneForUserIdWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToUserIdTable.updateMobilePhoneForUserId(curMobilePhone, newMobilePhone, userId);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateMobilePhoneForUserIdWithNonExistedUserId");
    }

    @Test
    public void testUpdateMobilePhoneForUserIdWithExistedMobilePhone() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        MobilePhone curMobilePhone = new MobilePhone("+11111111111", "US");
        MobilePhone newMobilePhone = new MobilePhone("+12222222222", "US");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(newMobilePhone, userId1);
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(curMobilePhone, userId2);

            mobilePhoneToUserIdTable.updateMobilePhoneForUserId(curMobilePhone, newMobilePhone, userId2);
        } catch (DuplicateKeyException error) {
            assertEquals(userId1, mobilePhoneToUserIdTable.getUserId(newMobilePhone));
            assertEquals(userId2, mobilePhoneToUserIdTable.getUserId(curMobilePhone));
            return;

        } finally {
            mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(newMobilePhone, userId1);
            mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(curMobilePhone, userId2);
        }
        fail("Failed while running testUpdateMobilePhoneForUserIdWithExistedMobilePhone");
    }

    // This is same as testCreateMobilePhoneForUserIdHappyCase
    @Test
    public void testGetUserIdHappyCase() {}

    @Test
    public void testGetUserIdWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToUserIdTable.getUserId(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithInvalidRequest");
    }

    @Test
    public void testGetUserIdWithNonExistedMobilePhone() 
            throws ValidationException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        try {
            mobilePhoneToUserIdTable.getUserId(mobilePhone);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithNonExistedMobilePhone");
    }

    @Test
    public void testGetMobilePhoneHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        MobilePhone mobilePhone = new MobilePhone("+11111111111", "US");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToUserIdTable.createMobilePhoneForUserId(mobilePhone, userId);
            MobilePhone persistedMobilePhone = mobilePhoneToUserIdTable.getMobilePhone(userId, false);
            assertEquals(mobilePhone, persistedMobilePhone);

        } finally {
            try {
                mobilePhoneToUserIdTable.deleteMobilePhoneForUserId(mobilePhone, userId);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetMobilePhoneWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            mobilePhoneToUserIdTable.getMobilePhone(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetMobilePhoneWithInvalidRequest");
    }

    @Test
    public void testGetMobilePhoneWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            mobilePhoneToUserIdTable.getMobilePhone(userId, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetMobilePhoneWithNonExistedUserId");
    }

    @AfterClass
    public static void tearDownMobilePhoneToUserIdTable() throws RepositoryClientException, RepositoryServerException {
//        mobilePhoneToUserIdTable.deleteTable();
    }
}
