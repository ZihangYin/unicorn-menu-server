package com.unicorn.rest.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.server.injector.TestRepositoryTableBinder;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class CustomerRepositoryImplTest {

    private static TestRepositoryTableBinder testRepositoryTableBinder;
    private static CustomerRepositoryImpl customerRepositoryImpl;

    @BeforeClass
    public static void setUpRepository() throws Exception {
        testRepositoryTableBinder = new TestRepositoryTableBinder();
        customerRepositoryImpl = new CustomerRepositoryImpl(testRepositoryTableBinder.getMockedDynamoCustomerProfileTable(),
                testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable());
    }

    @After
    public void clearMockedRepository() {
        /*
         * Reset the mocking on this object so that the field can be safely re-used between tests.
         */
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoCustomerProfileTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoMobilePhoneToPrincipalTable());
        Mockito.reset(testRepositoryTableBinder.getMockedDynamoEmailAddressToPrincipalTable());
    }

    private void mockGetCustomerPrincipalFromCustomerNameHappyCase(Name customerName, Long expectedCustomerPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doReturn(expectedCustomerPrincipal).when(mockedDynamoNameToPrincipalTable).getCurrentPrincipal(customerName);
    }

    private void mockGetCustomerPrincipalFromCustomerNameNoCustomerName(Name customerName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = testRepositoryTableBinder.getMockedDynamoNameToPrincipalTable();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedDynamoNameToPrincipalTable).getCurrentPrincipal(customerName);
    }

    /*
     * Happy Case
     */
    @Test
    public void testGetCustomerPrincipalFromCustomerNameHappyCase() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "customer_name";
        Long customerPrincipal = SimpleFlakeKeyGenerator.generateKey();

        mockGetCustomerPrincipalFromCustomerNameHappyCase(Name.validateCustomerName(loginName), customerPrincipal);
        assertEquals(customerPrincipal, customerRepositoryImpl.getPrincipalForLoginName(loginName));
    }

    /*
     * Bad Request
     */
    @Test
    public void testGetCustomerPrincipalFromCustomerNameDoesNotExist() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        String loginName = "customer_name";
        mockGetCustomerPrincipalFromCustomerNameNoCustomerName(Name.validateCustomerName(loginName));
        try {
            customerRepositoryImpl.getPrincipalForLoginName(loginName);
        } catch (ItemNotFoundException error) {
            return;
        }
        fail("Failed while running testGetCustomerPrincipalFromCustomerNameDoesNotExist");
    }
    /*
     * Internal Server Errors
     */
}
