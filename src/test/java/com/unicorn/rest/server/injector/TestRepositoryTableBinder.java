package com.unicorn.rest.server.injector;

import org.mockito.Mockito;

import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthenticationTokenTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoEmailAddressToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoMobilePhoneToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserNameToUserIdTable;
import com.unicorn.rest.repository.impl.dynamodb.DynamoUserProfileTable;

public class TestRepositoryTableBinder {

    private DynamoAuthenticationTokenTable mockedDynamoAuthenticationTokenTable = Mockito.mock(DynamoAuthenticationTokenTable.class);
    private DynamoUserNameToUserIdTable mockedDynamoUserNameToUserIdTable = Mockito.mock(DynamoUserNameToUserIdTable.class);
    private DynamoMobilePhoneToUserIdTable mockedDynamoMobilePhoneToUserIdTable = Mockito.mock(DynamoMobilePhoneToUserIdTable.class);
    private DynamoEmailAddressToUserIdTable mockedDynamoEmailAddressToUserIdTable = Mockito.mock(DynamoEmailAddressToUserIdTable.class);
    private DynamoUserProfileTable mockedDynamoUserProfileTable = Mockito.mock(DynamoUserProfileTable.class);

    public DynamoAuthenticationTokenTable getMockedDynamoAuthenticationTokenTable() {
        return mockedDynamoAuthenticationTokenTable;
    }

    public DynamoUserNameToUserIdTable getMockedDynamoUserNameToUserIdTable() {
        return mockedDynamoUserNameToUserIdTable;
    }
    
    public DynamoMobilePhoneToUserIdTable getMockedDynamoMobilePhoneToUserIdTable() {
        return mockedDynamoMobilePhoneToUserIdTable;
    }
    
    public DynamoEmailAddressToUserIdTable getMockedDynamoEmailAddressToUserIdTable() {
        return mockedDynamoEmailAddressToUserIdTable;
    }
    
    public DynamoUserProfileTable getMockedDynamoUserProfileTable() {
        return mockedDynamoUserProfileTable;
    }
}
