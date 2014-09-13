package com.unicorn.rest.server.injector;

import org.mockito.Mockito;

import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthorizationTokenTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoCustomerProfileTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;

public class TestRepositoryTableBinder {

    private DynamoAuthorizationTokenTable mockedDynamoAuthorizationTokenTable = Mockito.mock(DynamoAuthorizationTokenTable.class);
    private DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = Mockito.mock(DynamoNameToPrincipalTable.class);
    private DynamoMobilePhoneToPrincipalTable mockedDynamoMobilePhoneToPrincipalTable = Mockito.mock(DynamoMobilePhoneToPrincipalTable.class);
    private DynamoEmailAddressToPrincipalTable mockedDynamoEmailAddressToPrincipalTable = Mockito.mock(DynamoEmailAddressToPrincipalTable.class);
    private DynamoUserProfileTable mockedDynamoUserProfileTable = Mockito.mock(DynamoUserProfileTable.class);
    private DynamoCustomerProfileTable mockedDynamoCustomerProfileTable = Mockito.mock(DynamoCustomerProfileTable.class);

    public DynamoAuthorizationTokenTable getMockedDynamoAuthorizationTokenTable() {
        return mockedDynamoAuthorizationTokenTable;
    }

    public DynamoNameToPrincipalTable getMockedDynamoNameToPrincipalTable() {
        return mockedDynamoNameToPrincipalTable;
    }
    
    public DynamoMobilePhoneToPrincipalTable getMockedDynamoMobilePhoneToPrincipalTable() {
        return mockedDynamoMobilePhoneToPrincipalTable;
    }
    
    public DynamoEmailAddressToPrincipalTable getMockedDynamoEmailAddressToPrincipalTable() {
        return mockedDynamoEmailAddressToPrincipalTable;
    }
    
    public DynamoUserProfileTable getMockedDynamoUserProfileTable() {
        return mockedDynamoUserProfileTable;
    }
    
    public DynamoCustomerProfileTable getMockedDynamoCustomerProfileTable() {
        return mockedDynamoCustomerProfileTable;
    }
}
