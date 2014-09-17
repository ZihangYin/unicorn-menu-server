package com.unicorn.rest.repository.impl.dynamodb;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

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
import com.unicorn.rest.repository.table.UserProfileTable;

@Service
public class DynamoUserProfileTable implements UserProfileTable {
    private static final Logger LOG = LogManager.getLogger(DynamoUserProfileTable.class);

    private static final String USER_PRINCIPAL_KEY = "USER_PRINCIPAL";
    private static final String PASSWORD_KEY = "PASSWORD";
    private static final String SALT_KEY = "SALT";
    private static final String USER_DISPLAY_NAME_KEY = "USER_DISPLAY_NAME";

    private static final String USER_DISPLAY_NAME_GSI_KEY = "USER_DISPLAY_NAME-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();

    @Override
    public Long createUser(@Nullable Long userPrincipal, @Nullable DisplayName userDisplayName, @Nullable ByteBuffer password, @Nullable ByteBuffer salt) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (userPrincipal == null || userDisplayName == null || password == null || salt == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createUser, but received: userPrincipal=%s, password=%s, salt=%s, userDisplayName=%s.", 
                            userPrincipal, password, salt, userDisplayName));
        }
        createUserProfile(userPrincipal, password, salt, userDisplayName.getDisplayName());
        return userPrincipal;
    }

    @Override
    public @Nonnull PrincipalAuthenticationInfo getUserAuthenticationInfo(@Nullable Long userPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (userPrincipal == null) {
            throw new ValidationException("Expecting non-null request paramter for getUserAuthenticationInfo, but received: userPrincipal=null.");
        }

        Map<String, AttributeValue> userAttrs = getUserInfo(userPrincipal, PASSWORD_KEY, SALT_KEY);
        return PrincipalAuthenticationInfo.buildPrincipalAuthenticationInfo()
                .principal(userPrincipal).password(DynamoAttributeValueUtils.getRequiredByteBufferValue(userAttrs, PASSWORD_KEY))
                .salt(DynamoAttributeValueUtils.getRequiredByteBufferValue(userAttrs, SALT_KEY))
                .build();
    }

    private Map<String, AttributeValue> getUserInfo(@Nonnull Long userPrincipal, @Nullable String... attributesToGet) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(userPrincipal));

        GetItemRequest getItemRequest = new GetItemRequest().withTableName(USER_PROFILE_TABLE_NAME).withKey(key).withAttributesToGet(attributesToGet);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getUserInfo %s from table %s.", getItemRequest, USER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The user principal {} in the getUser request does not exist in the table.", userPrincipal);
            throw new ItemNotFoundException();
        }
        return getItemResult.getItem();
    }

    private void createUserProfile(@Nonnull Long userPrincipal, @Nonnull ByteBuffer password, @Nonnull ByteBuffer salt, @Nonnull String userDisplayName) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(USER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(userPrincipal));
        item.put(PASSWORD_KEY, DynamoAttributeValueUtils.byteBufferAttrValue(password));
        item.put(SALT_KEY, DynamoAttributeValueUtils.byteBufferAttrValue(salt));
        item.put(USER_DISPLAY_NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(userDisplayName));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(USER_PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(USER_PROFILE_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The user principal {} in createUserProfile request already existed.", userPrincipal);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createUserProfile %s to table %s.", putItemRequest, USER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteUser(@Nonnull Long userPrincipal) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(userPrincipal));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(USER_PROFILE_TABLE_NAME).withKey(key);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The user principal {} in deleteUser request does not match with one in table.", userPrincipal);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteUser %s from table %s.", deleteItemRequest, USER_PROFILE_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {

        GlobalSecondaryIndex userDisplayNameGSI = new GlobalSecondaryIndex()
        .withIndexName(USER_DISPLAY_NAME_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(USER_DISPLAY_NAME_KEY, KeyType.HASH)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(USER_PROFILE_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(USER_PRINCIPAL_KEY, ScalarAttributeType.N),
                new AttributeDefinition(USER_DISPLAY_NAME_KEY, ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement(USER_PRINCIPAL_KEY, KeyType.HASH))
                .withGlobalSecondaryIndexes(userDisplayNameGSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", USER_PROFILE_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(USER_PROFILE_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", USER_PROFILE_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
