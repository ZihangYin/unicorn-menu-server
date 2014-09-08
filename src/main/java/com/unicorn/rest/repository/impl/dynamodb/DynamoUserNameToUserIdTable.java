package com.unicorn.rest.repository.impl.dynamodb;

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
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
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
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.repository.table.UserNameToUserIdTable;
import com.unicorn.rest.utils.TimeUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public class DynamoUserNameToUserIdTable implements UserNameToUserIdTable {
    private static final Logger LOG = LogManager.getLogger(DynamoUserNameToUserIdTable.class);

    public static final String USER_NAME_TO_ID_TABLE_NAME = "USER_NAME_TO_ID_TABLE";
    public static final String USER_NAME_KEY = "USER_NAME"; //HashKey
    public static final String USER_ID_KEY = "USER_ID";
    public static final String ACTIVATE_IN_EPOCH_KEY = "ACTIVATE_IN_EPOCH"; 
    public static final String DEACTIVATE_IN_EPOCH_KEY = "DEACTIVATE_IN_EPOCH"; //RangeKey

    public static final String ACTIVATE_IN_EPOCH_LSI_KEY = "ACTIVATE_IN_EPOCH-LSI";
    public static final String USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY = "USER_ID-ACTIVATE_IN_EPOCH-GSI";
    public static final String USER_ID_DEACTIVATE_IN_EPOCH_GSI_KEY = "USER_ID-DEACTIVATE_IN_EPOCH-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();
    
    @EqualsAndHashCode()
    @ToString()
    @Getter
    public static class UserNameToUserIDItem {

        @Nonnull private final String userName;
        @Nonnull private final Long userId;
        private final Long activateTime;
        @Nonnull private final Long deactivateTime;

        public UserNameToUserIDItem(@Nonnull Map<String, AttributeValue> attributes) {
            this.userName = DynamoAttributeValueUtils.getRequiredStringValue(attributes, USER_NAME_KEY);
            this.userId = DynamoAttributeValueUtils.getRequiredLongValue(attributes, USER_ID_KEY);    
            this.activateTime = DynamoAttributeValueUtils.getRequiredLongValue(attributes, ACTIVATE_IN_EPOCH_KEY);
            this.deactivateTime = DynamoAttributeValueUtils.getRequiredLongValue(attributes, DEACTIVATE_IN_EPOCH_KEY);
        }
        
        public UserNameToUserIDItem(@Nonnull String userName, @Nonnull Long userId, @Nullable Long activateTime, @Nonnull Long deactivateTime) {
            this.userName = userName;
            this.userId = userId;
            this.activateTime = activateTime;
            this.deactivateTime = deactivateTime;
        }
    }

    @Override
    public void createUserNameForUserId(@Nullable UserName userName, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (userName == null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createUserNameForUserId, but received: userName=%s, userId=%s", userName, userId));
        }
        createUserNameForUserId(new UserNameToUserIDItem(userName.getUserName(), userId, TimeUtils.getEpochTimeNowInUTC(), Long.MAX_VALUE));
    }

    @Override
    public void updateUserNameForUserId(@Nullable UserName curUserName, @Nullable UserName newUserName, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        if (curUserName == null || newUserName == null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for updateUserNameForUserId, but received: curUserName=%s, newUserName=%s, userId=%s",
                    curUserName, newUserName, userId));
        } 
        String curUserNameStr = curUserName.getUserName();
        String newUserNameStr = newUserName.getUserName();
        
        UserNameToUserIDItem curUserNameToUserIDItem = queryUserForUserId(userId, false);
        if (!curUserNameStr.equals(curUserNameToUserIDItem.getUserName())) {
            throw new ItemNotFoundException();
        };

        Long now = TimeUtils.getEpochTimeNowInUTC();
        createUserNameForUserId(new UserNameToUserIDItem(newUserNameStr, userId, now, Long.MAX_VALUE));
        deactivateUserNameForUserId(curUserNameStr, userId, curUserNameToUserIDItem.getActivateTime(), now);
    }

    @Override
    public @Nonnull Long getCurrentUserId(@Nullable UserName userName) 
            throws ValidationException , ItemNotFoundException, RepositoryServerException {
        if (userName == null) {
            throw new ValidationException("Expecting non-null request paramter for getCurrentUserId, but received: userName=null");
        }
        return getCurrentUserIdForUserName(userName.getUserName());
    }

    @Override
    public @Nonnull Long getUserIdAtTime(@Nullable UserName userName, @Nullable Long activeTime) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (userName == null || activeTime == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getUserIdAtTime, but received: userName=%s, activeTime=%s", userName, activeTime));
        }
        return getUserIdForUserNameAtTime(userName.getUserName(), activeTime);
    }

    @Override
    public @Nonnull String getUserName(@Nullable Long userId, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException {
        if (userId == null) {
            throw new ValidationException("Expecting non-null request paramter for getUserName, but received: userId=null");
        }
        return queryUserForUserId(userId, checkStaleness).getUserName();
    }
    
    /**
     * 
     * Deactivate user_ame for user_id
     * Create the current user_name to user_id with correct deactivate_time to user_name_to_id_table
     * and delete the user_name to user_id with deactivate_time as Long.MAX_VALUE
     * 
     * @Note:
     * If the first step succeeds and the second steps failed, there is a 
     * chance that user will have multiple user_names to log in. 
     * However, we can still figure out which one is the latest by checking the activate_in_epoch 
     * 
     * @param curUserName
     * @param userId
     * @param deactivateTime
     * @throws DuplicateKeyException if request is invalid
     * @throws ItemNotFoundException 
     * @throws RepositoryServerException internal server error
     */
    private void deactivateUserNameForUserId(@Nonnull String curUserName, @Nonnull Long userId,  @Nonnull Long curActivateTime, @Nonnull Long deactivateTime) 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        createUserNameForUserId(new UserNameToUserIDItem(curUserName, userId, curActivateTime, deactivateTime));
        deleteUserNameForUserId(new UserNameToUserIDItem(curUserName, userId, curActivateTime, Long.MAX_VALUE));
    }

    private @Nonnull Long getCurrentUserIdForUserName(@Nonnull String userName) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(userName));
        key.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(Long.MAX_VALUE));

        GetItemRequest getItemRequest = new GetItemRequest().
                withTableName(USER_NAME_TO_ID_TABLE_NAME).withKey(key).withAttributesToGet(USER_ID_KEY);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.getItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getCurrentUserIdForUserName %s from table %s.", getItemRequest, USER_NAME_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.warn( String.format("The user name %s in the getCurrentUserIdForUserName request %s does not exist in the table.", userName, getItemRequest));
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(getItemResult.getItem(), USER_ID_KEY);
    }

    private @Nonnull Long getUserIdForUserNameAtTime(@Nonnull String userName, @Nonnull Long activeTime) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(USER_NAME_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.stringAttrValue(userName)));
        keyConditions.put(ACTIVATE_IN_EPOCH_KEY, new Condition().withComparisonOperator(ComparisonOperator.LT)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(activeTime)));

        QueryRequest queryRequest = new QueryRequest().withTableName(USER_NAME_TO_ID_TABLE_NAME).withIndexName(ACTIVATE_IN_EPOCH_LSI_KEY)
                .withKeyConditions(keyConditions).withScanIndexForward(false).withLimit(1);
        
        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getUserIdForUserNameAtTime %s from table %s.", queryRequest, USER_NAME_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.warn( String.format("The user name %s with active time %s in the getUserIdForUserNameAtTime request %s does not exist in the table.", 
                    userName, activeTime, queryResult));
            throw new ItemNotFoundException();
        }
        if (activeTime > DynamoAttributeValueUtils.getRequiredLongValue(queryResult.getItems().get(0), DEACTIVATE_IN_EPOCH_KEY)) {
            LOG.warn( String.format("The user name %s with active time %s in the getUserIdForUserNameAtTime request %s does not exist in the table.", 
                    userName, activeTime, queryResult));
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(queryResult.getItems().get(0), USER_ID_KEY);
    }

    private void createUserNameForUserId(@Nonnull UserNameToUserIDItem userNameToUserIDItem) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(USER_NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(userNameToUserIDItem.getUserName()));
        item.put(USER_ID_KEY, DynamoAttributeValueUtils.numberAttrValue(userNameToUserIDItem.getUserId()));
        item.put(ACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(userNameToUserIDItem.getActivateTime()));
        item.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(userNameToUserIDItem.getDeactivateTime()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(USER_NAME_KEY, DynamoAttributeValueUtils.expectEmpty());
        expected.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(USER_NAME_TO_ID_TABLE_NAME).withItem(item).withExpected(expected);
        
        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.warn( String.format("The user name %s in createUserNameForUserId request %s already existed.", 
                    userNameToUserIDItem.getUserName(), putItemRequest), error);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createUserNameForUserId %s to table %s.", putItemRequest, USER_NAME_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteUserNameForUserId(@Nonnull UserNameToUserIDItem userNameToUserIDItem) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(USER_NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(userNameToUserIDItem.getUserName()));
        key.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(userNameToUserIDItem.getDeactivateTime()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(USER_ID_KEY, DynamoAttributeValueUtils.expectEqual(DynamoAttributeValueUtils.numberAttrValue(userNameToUserIDItem.getUserId())));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(USER_NAME_TO_ID_TABLE_NAME).withKey(key).withExpected(expected);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException | ConditionalCheckFailedException error) {
            LOG.warn( String.format("The user id %s in deleteUserNameForUserId request %s does not match with one in table.", 
                    userNameToUserIDItem.getUserId(), deleteItemRequest), error);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteUserNameForUserId %s from table %s.", deleteItemRequest, USER_NAME_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected @Nonnull UserNameToUserIDItem queryUserForUserId(@Nonnull Long userId, boolean checkStaleness) 
            throws ItemNotFoundException, StaleDataException, RepositoryServerException {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(USER_ID_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(userId)));

        QueryRequest queryRequest = new QueryRequest().withTableName(USER_NAME_TO_ID_TABLE_NAME).withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
                .withKeyConditions(keyConditions).withScanIndexForward(false).withLimit(1);
        
        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to queryUserForUserId %s from table %s.", queryRequest, USER_NAME_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.warn( String.format("The user id %s in the queryUserForUserId request %s does not exist in the table.", userId, queryResult));
            throw new ItemNotFoundException();
        }

        UserNameToUserIDItem userNameToUserIDItem = new UserNameToUserIDItem(queryResult.getItems().get(0));
        if (!checkStaleness) {
            return userNameToUserIDItem;
        }

        String userName = userNameToUserIDItem.getUserName();
        try {
            Long userIdForUserName = getCurrentUserIdForUserName(userName);
            if (userId.equals(userIdForUserName)) {
                return userNameToUserIDItem;
            }
        } catch (ItemNotFoundException error) {}
        LOG.warn( String.format("Found stale user name for user id %s", userName, userId));
        throw new StaleDataException();
    }
    
    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        
        LocalSecondaryIndex activateInEpochLSI = new LocalSecondaryIndex()
        .withIndexName(ACTIVATE_IN_EPOCH_LSI_KEY)
        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
        .withKeySchema(
                new KeySchemaElement(USER_NAME_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );
        
        GlobalSecondaryIndex userIdActivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(USER_ID_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );
        
        GlobalSecondaryIndex userIdDeactivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(USER_ID_DEACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(USER_ID_KEY, KeyType.HASH),
                new KeySchemaElement(DEACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(USER_NAME_TO_ID_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(USER_NAME_KEY, ScalarAttributeType.S),
                new AttributeDefinition(USER_ID_KEY, ScalarAttributeType.N),
                new AttributeDefinition(ACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N),
                new AttributeDefinition(DEACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N))
                .withKeySchema(new KeySchemaElement(USER_NAME_KEY, KeyType.HASH), 
                        new KeySchemaElement(DEACTIVATE_IN_EPOCH_KEY, KeyType.RANGE))
                .withGlobalSecondaryIndexes(userIdActivateInEpochGSI, userIdDeactivateInEpochGSI)
                .withLocalSecondaryIndexes(activateInEpochLSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", USER_NAME_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(USER_NAME_TO_ID_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", USER_NAME_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
