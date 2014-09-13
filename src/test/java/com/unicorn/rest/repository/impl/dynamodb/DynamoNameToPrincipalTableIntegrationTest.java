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
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable.NameToPrincipalItem;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;
import com.unicorn.rest.utils.TimeUtils;

public class DynamoNameToPrincipalTableIntegrationTest {

    private static DynamoNameToPrincipalTable nameToPrincipalTable;

    @BeforeClass
    public static void setUpNameToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
        nameToPrincipalTable = new DynamoNameToPrincipalTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        nameToPrincipalTable.createTable();
    }

    @Test
    public void testCreateNameForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name name = Name.validateUserName("username");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.createNameForPrincipal(name, principal);
            Long persistedPrincipal = nameToPrincipalTable.getCurrentPrincipal(name);
            assertEquals(principal, persistedPrincipal);

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(name.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateNameForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            nameToPrincipalTable.createNameForPrincipal(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateNameForPrincipalWithInvalidRequest");
    }

    @Test
    public void testCreateNameForPrincipalWithExistedName() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name name = Name.validateUserName("username");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.createNameForPrincipal(name, principal1);
            try {
                nameToPrincipalTable.createNameForPrincipal(name, principal2);
            } catch(DuplicateKeyException error) {
                Long persistedPrincipal = nameToPrincipalTable.getCurrentPrincipal(name);
                assertEquals(principal1, persistedPrincipal);
                return;
            }
            fail("Failed while running testCreateNameForPrincipalWithExistedName");

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(name.getName(), principal1, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateNameForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.createNameForPrincipal(curName, principal);
            nameToPrincipalTable.updateNameForPrincipal(curName, newName, principal);
            try {
                nameToPrincipalTable.getCurrentPrincipal(curName);
            } catch (ItemNotFoundException error) {
                NameToPrincipalItem newUser = nameToPrincipalTable.queryNameForPrincipal(principal, false);
                Long persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(newName, newUser.getActivateTime() + 10);
                assertEquals(principal, persistedPrincipal);
                persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(curName, newUser.getActivateTime() - 10);
                assertEquals(principal, persistedPrincipal);

                String persistedNewName = nameToPrincipalTable.getName(principal, false);
                assertEquals(newName.getName(), persistedNewName);
                return;
            }
            fail("Failed while running testUpdateNameForPrincipalHappyCase");

        } finally {
            try {
                NameToPrincipalItem newUser = nameToPrincipalTable.queryNameForPrincipal(principal, false);
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(curName.getName(), principal, null, newUser.getActivateTime()));
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(newName.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testUpdateNameForPrincipalWithInvalidRequest() 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        try {
            nameToPrincipalTable.updateNameForPrincipal(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testUpdateNameForPrincipalWithInvalidRequest");
    }

    @Test
    public void testUpdateNameForPrincipalWithUnexpectedCurName() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            nameToPrincipalTable.createNameForPrincipal(curName, principal);
            nameToPrincipalTable.updateNameForPrincipal(Name.validateUserName("wrongusername"), newName, principal);
        } catch (ItemNotFoundException error) {
            assertEquals(principal, nameToPrincipalTable.getCurrentPrincipal(curName));
            return;

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(curName.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }

        fail("Failed while running testUpdateNameForPrincipalWithUnexpectedCurName");
    }

    @Test
    public void testUpdateNameForPrincipalWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        try {
            nameToPrincipalTable.updateNameForPrincipal(curName, newName, principal);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testUpdateNameForPrincipalWithNonExistedPrincipal");
    }

    @Test
    public void testUpdateNameForPrincipalWithExistedName() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Long principal1 = SimpleFlakeKeyGenerator.generateKey();
        Long principal2 = SimpleFlakeKeyGenerator.generateKey();

        try {
            nameToPrincipalTable.createNameForPrincipal(newName, principal1);
            nameToPrincipalTable.createNameForPrincipal(curName, principal2);

            nameToPrincipalTable.updateNameForPrincipal(curName, newName, principal2);
        } catch (DuplicateKeyException error) {
            assertEquals(principal1, nameToPrincipalTable.getCurrentPrincipal(newName));
            assertEquals(principal2, nameToPrincipalTable.getCurrentPrincipal(curName));
            return;

        } finally {
            nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(newName.getName(), principal1, null, Long.MAX_VALUE));
            nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(curName.getName(), principal2, null, Long.MAX_VALUE));
        }
        fail("Failed while running testUpdateNameForPrincipalWithExistedName");
    }

    @Test
    public void testGetCurrentPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.createNameForPrincipal(curName, principal);
            Long persistedPrincipal = nameToPrincipalTable.getCurrentPrincipal(curName);
            assertEquals(principal, persistedPrincipal);

            nameToPrincipalTable.updateNameForPrincipal(curName, newName, principal);
            persistedPrincipal = nameToPrincipalTable.getCurrentPrincipal(newName);
            assertEquals(principal, persistedPrincipal);

        } finally {
            try {
                NameToPrincipalItem newUser = nameToPrincipalTable.queryNameForPrincipal(principal, false);
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(curName.getName(), principal, null, newUser.getActivateTime()));
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(newName.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetCurrentPrincipalWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            nameToPrincipalTable.getCurrentPrincipal(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithInvalidRequest");
    }

    @Test
    public void testGetCurrentPrincipalWithNonExistedName() 
            throws ValidationException, RepositoryServerException {
        Name name = Name.validateUserName("username");
        try {
            nameToPrincipalTable.getCurrentPrincipal(name);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalWithNonExistedName");
    }

    // This is same as testUpdateNameForPrincipalHappyCase
    @Test
    public void testGetPrincipalAtTimeHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name curName = Name.validateUserName("curusername");
        Name newName = Name.validateUserName("newusername");
        Name newerName = Name.validateUserName("newerusername");
        Long principal = SimpleFlakeKeyGenerator.generateKey();

        NameToPrincipalItem newUser = null;
        NameToPrincipalItem newerUser = null;
        try {
            nameToPrincipalTable.createNameForPrincipal(curName, principal);
            nameToPrincipalTable.updateNameForPrincipal(curName, newName, principal);
            newUser = nameToPrincipalTable.queryNameForPrincipal(principal, false);
            nameToPrincipalTable.updateNameForPrincipal(newName, newerName, principal);
            newerUser = nameToPrincipalTable.queryNameForPrincipal(principal, false);

            Long persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(curName, newUser.getActivateTime() - 10);
            assertEquals(principal, persistedPrincipal);
            persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(newName, newUser.getActivateTime() + 10);
            assertEquals(principal, persistedPrincipal);
            persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(newName, newerUser.getActivateTime() - 10);
            assertEquals(principal, persistedPrincipal);
            persistedPrincipal = nameToPrincipalTable.getPrincipalAtTime(newerName, newerUser.getActivateTime() + 10);
            assertEquals(principal, persistedPrincipal);

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(curName.getName(), principal, null, newUser.getActivateTime()));
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(newName.getName(), principal, null, newerUser.getActivateTime()));
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(newerName.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }     
    }

    @Test
    public void testGetPrincipalAtTimeWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            nameToPrincipalTable.getPrincipalAtTime(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetPrincipalAtTimeWithInvalidRequest");
    }

    @Test
    public void testGetPrincipalAtTimeWithNonExistedName() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Name name = Name.validateUserName("username");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        Long nonExistedTime = TimeUtils.getEpochTimeNowInUTC();
        try {
            nameToPrincipalTable.createNameForPrincipal(name, principal);
            Long persistedPrincipal = nameToPrincipalTable.getCurrentPrincipal(name);
            assertEquals(principal, persistedPrincipal);

            nameToPrincipalTable.getPrincipalAtTime(name, nonExistedTime);
        } catch (ItemNotFoundException error) {
            return;

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(name.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        } 
        fail("Failed while running testGetPrincipalAtTimeWithNonExistedName");
    }

    @Test
    public void testGetNameHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Name name = Name.validateUserName("username");
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.createNameForPrincipal(name, principal);
            String persistedName = nameToPrincipalTable.getName(principal, false);
            assertEquals(name.getName(), persistedName);

        } finally {
            try {
                nameToPrincipalTable.deleteNameForPrincipal(new NameToPrincipalItem(name.getName(), principal, null, Long.MAX_VALUE));
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetNameWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            nameToPrincipalTable.getName(null, false);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetNameWithInvalidRequest");
    }

    @Test
    public void testGetNameWithNonExistedPrincipal() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        try {
            nameToPrincipalTable.getName(principal, false);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetNameWithNonExistedPrincipal");
    }

    @AfterClass
    public static void tearDownNameToPrincipalTable() throws RepositoryClientException, RepositoryServerException {
//        nameToPrincipalTable.deleteTable();
    }
}
