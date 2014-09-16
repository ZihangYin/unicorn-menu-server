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
import com.unicorn.rest.repository.impl.dynamodb.DynamoCustomerProfileTable;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.utils.AuthenticationSecretUtils;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class DynamoCustomerProfileTableIntegrationTest {

    private static DynamoCustomerProfileTable customerProfileTable;

    @BeforeClass
    public static void setUpCustomerProfileTable() throws RepositoryClientException, RepositoryServerException {
        customerProfileTable = new DynamoCustomerProfileTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        customerProfileTable.createTable();
    }

    @Test
    public void testCreateCustomerHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Long customerPrincipal = SimpleFlakeKeyGenerator.generateKey();
        DisplayName customerDisplayName = DisplayName.validateCustomerDisplayName("customerDisplayName");
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt);

        try {
            customerProfileTable.createCustomer(customerPrincipal, customerDisplayName, password, salt);
            PrincipalAuthenticationInfo customerAuthenticationInfo = customerProfileTable.getCustomerAuthenticationInfo(customerPrincipal);
            assertEquals(customerPrincipal, customerAuthenticationInfo.getPrincipal());
            assertEquals(password, customerAuthenticationInfo.getPassword());
            assertEquals(salt, customerAuthenticationInfo.getSalt());

        } finally {
            try {
                customerProfileTable.deleteCustomer(customerPrincipal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testCreateCustomerWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            customerProfileTable.createCustomer(null, null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testCreateCustomerWithInvalidRequest");
    }

    @Test
    public void testCreateCustomerWithExistedCustomerPrincipal() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Long customerPrincipal = SimpleFlakeKeyGenerator.generateKey();
        DisplayName customerDisplayName1 = DisplayName.validateCustomerDisplayName("customerDisplayNameOne");
        DisplayName customerDisplayName2 = DisplayName.validateCustomerDisplayName("customerDisplayNameTwo");
        ByteBuffer salt1 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer salt2 = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer password1 = AuthenticationSecretUtils.generateHashedSecretWithSalt("1a2b3c", salt1);
        ByteBuffer password2 = AuthenticationSecretUtils.generateHashedSecretWithSalt("3c2b1a", salt2);
        try {
            customerProfileTable.createCustomer(customerPrincipal, customerDisplayName1, password1, salt1);
            try {
                customerProfileTable.createCustomer(customerPrincipal, customerDisplayName2, password2, salt2);
            } catch(DuplicateKeyException error) {
                PrincipalAuthenticationInfo customerAuthenticationInfo = customerProfileTable.getCustomerAuthenticationInfo(customerPrincipal);
                assertEquals(customerPrincipal, customerAuthenticationInfo.getPrincipal());
                assertEquals(password1, customerAuthenticationInfo.getPassword());
                assertEquals(salt1, customerAuthenticationInfo.getSalt());
                return;
            }
            fail("Failed while running testCreateCustomerWithExistedCustomerPrincipal");

        } finally {
            try {
                customerProfileTable.deleteCustomer(customerPrincipal);
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    // This is same as testCreateCustomerHappyCase
    @Test
    public void testGetCustomerAuthenticationInfoHappyCase() {}

    @Test
    public void testGetCustomerAuthenticationInfoWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            customerProfileTable.getCustomerAuthenticationInfo(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetCustomerAuthenticationInfoHappyCase");
    }

    @Test
    public void testGetCustomerAuthenticationInfoWithNonExistedCustomer() 
            throws ValidationException, RepositoryServerException {
        Long customerPrincipal = SimpleFlakeKeyGenerator.generateKey();
        try {
            customerProfileTable.getCustomerAuthenticationInfo(customerPrincipal);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetCustomerAuthenticationInfoWithNonExistedCustomer");
    }

    @AfterClass
    public static void tearDownCustomerProfileTable() throws RepositoryClientException, RepositoryServerException {
//        customerProfileTable.deleteTable();
    }
}
