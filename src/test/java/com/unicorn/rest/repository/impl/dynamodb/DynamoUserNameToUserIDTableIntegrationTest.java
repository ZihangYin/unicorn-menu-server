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
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserNameToUserIdTable.UserNameToUserIDItem;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;
import com.unicorn.rest.utils.TimeUtils;

public class DynamoUserNameToUserIDTableIntegrationTest {

    private static DynamoUserNameToUserIdTable userNameToUserIdTable;

    @BeforeClass
    public static void setUpUserNameToUserIdTable() throws RepositoryClientException, RepositoryServerException {
        userNameToUserIdTable = new DynamoUserNameToUserIdTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        userNameToUserIdTable.createTable();
    }

    @Test
    public void testCreateUserNameForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName userName = new UserName("username");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.createUserNameForUserId(userName, userId);
            Long persistedUserId = userNameToUserIdTable.getCurrentUserId(userName);
            assertEquals(userId, persistedUserId);

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(userName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateUserNameForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            userNameToUserIdTable.createUserNameForUserId(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateUserNameForUserIdWithInvalidRequest");
    }

    @Test
    public void testCreateUserNameForUserIdWithExistedUserName() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName userName = new UserName("username");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.createUserNameForUserId(userName, userId1);
            try {
                userNameToUserIdTable.createUserNameForUserId(userName, userId2);
            } catch(DuplicateKeyException error) {
                Long persistedUserId = userNameToUserIdTable.getCurrentUserId(userName);
                assertEquals(userId1, persistedUserId);
                return;
            }
            fail("Failed while running testCreateUserNameForUserIdWithExistedUserName");

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(userName.getUserName(), userId1, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateUserNameForUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.createUserNameForUserId(curUserName, userId);
            userNameToUserIdTable.updateUserNameForUserId(curUserName, newUserName, userId);
            try {
                userNameToUserIdTable.getCurrentUserId(curUserName);
            } catch (ItemNotFoundException error) {
                UserNameToUserIDItem newUser = userNameToUserIdTable.queryUserForUserId(userId, false);
                Long persistedUserId = userNameToUserIdTable.getUserIdAtTime(newUserName, newUser.getActivateTime() + 10);
                assertEquals(userId, persistedUserId);
                persistedUserId = userNameToUserIdTable.getUserIdAtTime(curUserName, newUser.getActivateTime() - 10);
                assertEquals(userId, persistedUserId);

                String persistedNewUserName = userNameToUserIdTable.getUserName(userId, false);
                assertEquals(newUserName.getUserName(), persistedNewUserName);
                return;
            }
            fail("Failed while running testUpdateUserNameForUserIdHappyCase");

        } finally {
            try {
                UserNameToUserIDItem newUser = userNameToUserIdTable.queryUserForUserId(userId, false);
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(curUserName.getUserName(), userId, null, newUser.getActivateTime()));
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(newUserName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateUserNameForUserIdWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            userNameToUserIdTable.updateUserNameForUserId(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateUserNameForUserIdWithInvalidRequest");
    }

    @Test
    public void testUpdateUserNameForUserIdWithUnexpectedCurUserName() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            userNameToUserIdTable.createUserNameForUserId(curUserName, userId);
            userNameToUserIdTable.updateUserNameForUserId(new UserName("wrongusername"), newUserName, userId);
        } catch (ItemNotFoundException error) {
            assertEquals(userId, userNameToUserIdTable.getCurrentUserId(curUserName));
            return;

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(curUserName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }

        fail("Failed while running testUpdateUserNameForUserIdWithUnexpectedCurUserName");
    }

    @Test
    public void testUpdateUserNameForUserIdWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        try {
            userNameToUserIdTable.updateUserNameForUserId(curUserName, newUserName, userId);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateUserNameForUserIdWithNonExistedUserId");
    }

    @Test
    public void testUpdateUserNameForUserIdWithExistedUserName() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        Long userId1 = SimpleFlakeKeyGenerator.generateKey();
        Long userId2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            userNameToUserIdTable.createUserNameForUserId(newUserName, userId1);
            userNameToUserIdTable.createUserNameForUserId(curUserName, userId2);

            userNameToUserIdTable.updateUserNameForUserId(curUserName, newUserName, userId2);
        } catch (DuplicateKeyException error) {
            assertEquals(userId1, userNameToUserIdTable.getCurrentUserId(newUserName));
            assertEquals(userId2, userNameToUserIdTable.getCurrentUserId(curUserName));
            return;

        } finally {
            userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(newUserName.getUserName(), userId1, null, Long.MAX_VALUE));
            userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(curUserName.getUserName(), userId2, null, Long.MAX_VALUE));
        }
        fail("Failed while running testUpdateUserNameForUserIdWithExistedUserName");
    }

    @Test
    public void testGetCurrentUserIdHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.createUserNameForUserId(curUserName, userId);
            Long persistedUserId = userNameToUserIdTable.getCurrentUserId(curUserName);
            assertEquals(userId, persistedUserId);

            userNameToUserIdTable.updateUserNameForUserId(curUserName, newUserName, userId);
            persistedUserId = userNameToUserIdTable.getCurrentUserId(newUserName);
            assertEquals(userId, persistedUserId);

        } finally {
            try {
                UserNameToUserIDItem newUser = userNameToUserIdTable.queryUserForUserId(userId, false);
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(curUserName.getUserName(), userId, null, newUser.getActivateTime()));
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(newUserName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetCurrentUserIdWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            userNameToUserIdTable.getCurrentUserId(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithInvalidRequest");
    }

    @Test
    public void testGetCurrentUserIdWithNonExistedUserName() 
            throws ValidationException, RepositoryServerException {
        UserName userName = new UserName("username");
        try {
            userNameToUserIdTable.getCurrentUserId(userName);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetUserIdWithNonExistedUserName");
    }

    // This is same as testUpdateUserNameForUserIdHappyCase
    @Test
    public void testGetUserIdAtTimeHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName curUserName = new UserName("curusername");
        UserName newUserName = new UserName("newusername");
        UserName newerUserName = new UserName("newerusername");
        Long userId = SimpleFlakeKeyGenerator.generateKey();

        UserNameToUserIDItem newUser = null;
        UserNameToUserIDItem newerUser = null;
        try {
            userNameToUserIdTable.createUserNameForUserId(curUserName, userId);
            userNameToUserIdTable.updateUserNameForUserId(curUserName, newUserName, userId);
            newUser = userNameToUserIdTable.queryUserForUserId(userId, false);
            userNameToUserIdTable.updateUserNameForUserId(newUserName, newerUserName, userId);
            newerUser = userNameToUserIdTable.queryUserForUserId(userId, false);

            Long persistedUserId = userNameToUserIdTable.getUserIdAtTime(curUserName, newUser.getActivateTime() - 10);
            assertEquals(userId, persistedUserId);
            persistedUserId = userNameToUserIdTable.getUserIdAtTime(newUserName, newUser.getActivateTime() + 10);
            assertEquals(userId, persistedUserId);
            persistedUserId = userNameToUserIdTable.getUserIdAtTime(newUserName, newerUser.getActivateTime() - 10);
            assertEquals(userId, persistedUserId);
            persistedUserId = userNameToUserIdTable.getUserIdAtTime(newerUserName, newerUser.getActivateTime() + 10);
            assertEquals(userId, persistedUserId);

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(curUserName.getUserName(), userId, null, newUser.getActivateTime()));
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(newUserName.getUserName(), userId, null, newerUser.getActivateTime()));
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(newerUserName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }     
    }

    @Test
    public void testGetUserIdAtTimeWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            userNameToUserIdTable.getUserIdAtTime(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserIdAtTimeWithInvalidRequest");
    }

    @Test
    public void testGetUserIdAtTimeWithNonExistedUserName() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        UserName userName = new UserName("username");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        Long nonExistedTime = TimeUtils.getEpochTimeNowInUTC();
        try {
            userNameToUserIdTable.createUserNameForUserId(userName, userId);
            Long persistedUserId = userNameToUserIdTable.getCurrentUserId(userName);
            assertEquals(userId, persistedUserId);

            userNameToUserIdTable.getUserIdAtTime(userName, nonExistedTime);
        } catch (ItemNotFoundException error) {
            return;

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(userName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        } 
        fail("Failed while running testGetUserIdAtTimeWithNonExistedUserName");
    }

    @Test
    public void testGetUserNameHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        UserName userName = new UserName("username");
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.createUserNameForUserId(userName, userId);
            String persistedUserName = userNameToUserIdTable.getUserName(userId, false);
            assertEquals(userName.getUserName(), persistedUserName);

        } finally {
            try {
                userNameToUserIdTable.deleteUserNameForUserId(new UserNameToUserIDItem(userName.getUserName(), userId, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetUserNameWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            userNameToUserIdTable.getUserName(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetUserNameWithInvalidRequest");
    }

    @Test
    public void testGetUserNameWithNonExistedUserId() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        try {
            userNameToUserIdTable.getUserName(userId, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetUserNameWithNonExistedUserId");
    }

    @AfterClass
    public static void tearDownUserNameToUserIdTable() throws RepositoryClientException, RepositoryServerException {
//        userNameToUserIdTable.deleteTable();
    }
}
