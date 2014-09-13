package com.unicorn.rest.server.injector;

import org.mockito.Mockito;

import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthenticationTokenTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoNameToPrincipalTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;

public class TestRepositoryTableBinder {

    private DynamoAuthenticationTokenTable mockedDynamoAuthenticationTokenTable = Mockito.mock(DynamoAuthenticationTokenTable.class);
    private DynamoNameToPrincipalTable mockedDynamoNameToPrincipalTable = Mockito.mock(DynamoNameToPrincipalTable.class);
    private DynamoMobilePhoneToPrincipalTable mockedDynamoMobilePhoneToPrincipalTable = Mockito.mock(DynamoMobilePhoneToPrincipalTable.class);
    private DynamoEmailAddressToPrincipalTable mockedDynamoEmailAddressToPrincipalTable = Mockito.mock(DynamoEmailAddressToPrincipalTable.class);
    private DynamoUserProfileTable mockedDynamoUserProfileTable = Mockito.mock(DynamoUserProfileTable.class);

    public DynamoAuthenticationTokenTable getMockedDynamoAuthenticationTokenTable() {
        return mockedDynamoAuthenticationTokenTable;
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
}
