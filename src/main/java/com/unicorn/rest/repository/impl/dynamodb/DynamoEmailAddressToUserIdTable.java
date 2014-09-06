package com.unicorn.rest.repository.impl.dynamodb;

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
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
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
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.StaleDataException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.EmailAddress;
import com.unicorn.rest.repository.table.EmailAddressToUserIdTable;
import com.unicorn.rest.utils.TimeUtils;

@Service
public class DynamoEmailAddressToUserIdTable implements EmailAddressToUserIdTable {
    private static final Logger LOG = LogManager.getLogger(DynamoEmailAddressToUserIdTable.class);

    private static final String EMAIL_ADDRESS_TO_ID_TABLE_NAME = "EMAIL_ADDRESS_TO_ID_TABLE";
    private static final String EMAIL_ADDRESS_KEY = "EMAIL_ADDRESS"; //HashKey
    private static final String USER_ID_KEY = "USER_ID";
    private static final String ACTIVATE_IN_EPOCH_KEY = "ACTIVATE_IN_EPOCH"; 

    private static final String USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY = "USER_ID-ACTIVATE_IN_EPOCH-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();
    
    public void createEmailAddressForUserId(@Nullable EmailAddress emailAddress, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (emailAddress == null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createEmailAddressForUserId, but received: emailAddress=%s, userId=%s.", 
                            emailAddress, userId));
        }
        createEmailAddressForUserId(emailAddress.getEmailAddress(), userId, TimeUtils.getEpochTimeNowInUTC());
    } 

    public void updateEmailAddressForUserId(@Nullable EmailAddress curEmailAddress, @Nullable EmailAddress newEmailAddress, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        if (curEmailAddress == null|| newEmailAddress == null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for updateEmailAddressForUserId, but received: curEmailAddress=%s, newEmailAddress=%s, userId=%s.", 
                            curEmailAddress, newEmailAddress, userId));
        }
        
        String curEmailAddressStr = curEmailAddress.getEmailAddress();
        String newEmailAddressStr = newEmailAddress.getEmailAddress();
        
        if (!curEmailAddressStr.equals(getEmailAddress(userId, false))) {
            throw new ItemNotFoundException();
        };
        Long now = TimeUtils.getEpochTimeNowInUTC();
        createEmailAddressForUserId(newEmailAddressStr, userId, now);
        deleteEmailAddressForUserId(curEmailAddressStr, userId);
    }

    public @Nonnull Long getUserId(@Nullable EmailAddress emailAddress) 
            throws ValidationException , ItemNotFoundException, RepositoryServerException {
        if (emailAddress == null) {
            throw new ValidationException("Expecting non-null request paramter for getUserId, but received: emailAddress=null.");
        }
        return getUserIdForEmailAddress(emailAddress.getEmailAddress());
    }

    public @Nonnull String getEmailAddress(@Nullable Long userId, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException {
        if (userId == null) {
            throw new ValidationException("Expecting non-null request paramter for getEmailAddress, but received: userId=null.");
        }
        return queryEmailAddressForUser(userId, checkStaleness);
    }

    private @Nonnull Long getUserIdForEmailAddress(@Nonnull String emailAddress) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));

        GetItemRequest getItemRequest = new GetItemRequest().
                withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME).withKey(key).withAttributesToGet(USER_ID_KEY);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.getItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getUserIdForEmailAddress %s from table %s.", getItemRequest, EMAIL_ADDRESS_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.warn( String.format("The email address %s in the getUserIdForEmailAddress request %s does not exist in the table.", emailAddress, getItemRequest));
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(getItemResult.getItem(), USER_ID_KEY);

    }

    private void createEmailAddressForUserId(@Nonnull String emailAddress, @Nonnull Long userId, @Nonnull Long activateTime) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));
        item.put(USER_ID_KEY, DynamoAttributeValueUtils.numberAttrValue(userId));
        item.put(ACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(activateTime));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().
                withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.warn( String.format("The email address %s in createEmailAddressForUserId request %s already existed.", emailAddress, putItemRequest), error);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createEmailAddressForUserId %s to table %s.", putItemRequest, EMAIL_ADDRESS_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteEmailAddressForUserId(@Nonnull String emailAddress, @Nonnull Long userId) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(USER_ID_KEY, DynamoAttributeValueUtils.expectEqual(DynamoAttributeValueUtils.numberAttrValue(userId)));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME).withKey(key).withExpected(expected);

        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException | ConditionalCheckFailedException error) {
            LOG.warn( String.format("The user id %s in deleteEmailAddressForUserId request %s does not match with one in table.", userId, deleteItemRequest), error);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteEmailAddressForUserId %s from table %s.", deleteItemRequest, EMAIL_ADDRESS_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private @Nonnull String queryEmailAddressForUser(@Nonnull Long userId, boolean checkStaleness) 
            throws ItemNotFoundException, StaleDataException, RepositoryServerException {
        HashMap<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(USER_ID_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(userId)));

        QueryRequest queryRequest = new QueryRequest().withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME).withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
                .withKeyConditions(keyConditions).withAttributesToGet(EMAIL_ADDRESS_KEY).withScanIndexForward(false).withLimit(1);

        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to queryEmailAddressForUser %s from table %s.", queryRequest, EMAIL_ADDRESS_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.warn( String.format("The user id %s in the queryEmailAddressForUser request %s does not exist in the table.", userId, queryResult));
            throw new ItemNotFoundException();
        }
        String emailAddress = DynamoAttributeValueUtils.getRequiredStringValue(queryResult.getItems().get(0), EMAIL_ADDRESS_KEY);
        if (!checkStaleness) {
            return emailAddress;
        }
        try {
            Long userIdForEmailAddress = getUserIdForEmailAddress(emailAddress);
            if (userId.equals(userIdForEmailAddress)) {
                return emailAddress;
            }
        } catch (ItemNotFoundException error) {}
        LOG.warn( String.format("Found stale email address %s for user id %s", emailAddress, userId));
        
        throw new StaleDataException();
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        
        GlobalSecondaryIndex userIdActivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 2L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(USER_ID_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 2L))
        .withAttributeDefinitions(
                new AttributeDefinition(EMAIL_ADDRESS_KEY, ScalarAttributeType.S),
                new AttributeDefinition(USER_ID_KEY, ScalarAttributeType.N),
                new AttributeDefinition(ACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N))
                .withKeySchema(new KeySchemaElement(EMAIL_ADDRESS_KEY, KeyType.HASH))
                .withGlobalSecondaryIndexes(userIdActivateInEpochGSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", EMAIL_ADDRESS_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(EMAIL_ADDRESS_TO_ID_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", EMAIL_ADDRESS_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
