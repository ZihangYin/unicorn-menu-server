package com.unicorn.rest.repository.impl.dynamodb;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.repository.table.CustomerProfileTable;

public class DynamoCustomerProfileTable implements CustomerProfileTable {
    private static final Logger LOG = LogManager.getLogger(DynamoCustomerProfileTable.class);

    private static final String CUSTOMER_PRINCIPAL_KEY = "CUSTOMER_PRINCIPAL";
    private static final String PASSWORD_KEY = "PASSWORD";
    private static final String SALT_KEY = "SALT";
    private static final String CUSTOMER_DISPLAY_NAME_KEY = "CUSTOMER_DISPLAY_NAME";

    private static final String CUSTOMER_DISPLAY_NAME_GSI_KEY = "CUSTOMER_DISPLAY_NAME-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();

    @Override
    public Long createCustomer(@Nullable Long customerPrincipal, @Nullable DisplayName customerDisplayName, @Nullable ByteBuffer password, @Nullable ByteBuffer salt) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (customerPrincipal == null || customerDisplayName == null || password == null || salt == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createCustomer, but received: customerPrincipal=%s, password=%s, salt=%s, customerDisplayName=%s.", 
                            customerPrincipal, password, salt, customerDisplayName));
        }
        createCustomerProfile(customerPrincipal, password, salt, customerDisplayName.getDisplayName());
        return customerPrincipal;
    }

    @Override
    public @Nonnull PrincipalAuthenticationInfo getCustomerAuthenticationInfo(@Nullable Long customerPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (customerPrincipal == null) {
            throw new ValidationException("Expecting non-null request paramter for getCustomerAuthenticationInfo, but received: customerPrincipal=null.");
        }

        Map<String, AttributeValue> customerAttrs = getCustomerInfo(customerPrincipal, PASSWORD_KEY, SALT_KEY);
        return PrincipalAuthenticationInfo.buildPrincipalAuthenticationInfo()
                .principal(customerPrincipal).password(DynamoAttributeValueUtils.getRequiredByteBufferValue(customerAttrs, PASSWORD_KEY))
                .salt(DynamoAttributeValueUtils.getRequiredByteBufferValue(customerAttrs, SALT_KEY))
                .build();
    }

    private Map<String, AttributeValue> getCustomerInfo(@Nonnull Long customerPrincipal, @Nullable String... attributesToGet) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(CUSTOMER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(customerPrincipal));

        GetItemRequest getItemRequest = new GetItemRequest().withTableName(CUSTOMER_PROFILE_TABLE_NAME).withKey(key).withAttributesToGet(attributesToGet);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getCustomerInfo %s from table %s.", getItemRequest, CUSTOMER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The customer principal {} in the getCustomer request does not exist in the table.", customerPrincipal);
            throw new ItemNotFoundException();
        }
        return getItemResult.getItem();
    }

    private void createCustomerProfile(@Nonnull Long customerPrincipal, @Nonnull ByteBuffer password, @Nonnull ByteBuffer salt, @Nonnull String customerDisplayName) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(CUSTOMER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(customerPrincipal));
        item.put(PASSWORD_KEY, DynamoAttributeValueUtils.byteBufferAttrValue(password));
        item.put(SALT_KEY, DynamoAttributeValueUtils.byteBufferAttrValue(salt));
        item.put(CUSTOMER_DISPLAY_NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(customerDisplayName));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(CUSTOMER_PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(CUSTOMER_PROFILE_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The customer principal {} in createCustomerProfile request already existed.", customerPrincipal);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createCustomerProfile %s to table %s.", putItemRequest, CUSTOMER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteCustomer(@Nonnull Long customerPrincipal) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(CUSTOMER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(customerPrincipal));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(CUSTOMER_PROFILE_TABLE_NAME).withKey(key);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The customer principal {} in deleteCustomer request does not match with one in table.", customerPrincipal);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteCustomers %s from table %s.", deleteItemRequest, CUSTOMER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {

        GlobalSecondaryIndex customerDisplayNameGSI = new GlobalSecondaryIndex()
        .withIndexName(CUSTOMER_DISPLAY_NAME_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(CUSTOMER_DISPLAY_NAME_KEY, KeyType.HASH)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(CUSTOMER_PROFILE_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(CUSTOMER_PRINCIPAL_KEY, ScalarAttributeType.N),
                new AttributeDefinition(CUSTOMER_DISPLAY_NAME_KEY, ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement(CUSTOMER_PRINCIPAL_KEY, KeyType.HASH))
                .withGlobalSecondaryIndexes(customerDisplayNameGSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", CUSTOMER_PROFILE_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(CUSTOMER_PROFILE_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", CUSTOMER_PROFILE_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
