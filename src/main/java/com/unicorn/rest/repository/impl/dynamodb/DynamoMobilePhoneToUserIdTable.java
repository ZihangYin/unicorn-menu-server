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
import com.unicorn.rest.repository.model.MobilePhone;
import com.unicorn.rest.repository.table.MobilePhoneToUserIdTable;
import com.unicorn.rest.utils.TimeUtils;

@Service
public class DynamoMobilePhoneToUserIdTable implements MobilePhoneToUserIdTable {
    private static final Logger LOG = LogManager.getLogger(DynamoMobilePhoneToUserIdTable.class);

    private static final String MOBILE_PHONE_TO_ID_TABLE_NAME = "MOBILE_PHONE_TO_ID_TABLE";
    private static final String PHONE_NUMBER_KEY = "PHONE_NUMBER"; //HashKey
    private static final String COUNTRY_CODE_KEY = "COUNTRY_CODE"; //RangeKey
    private static final String USER_ID_KEY = "USER_ID";
    private static final String ACTIVATE_IN_EPOCH_KEY = "ACTIVATE_IN_EPOCH"; 

    private static final String USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY = "USER_ID-ACTIVATE_IN_EPOCH-GSI";
    private static final String COUNTRY_CODE_PHONE_NUMBER_GSI_KEY = "COUNTRY_CODE-PHONE_NUMBER-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();

    private MobilePhone buildMobilePhone(@Nonnull Map<String, AttributeValue> attributes) {
        Integer countryCode = DynamoAttributeValueUtils.getRequiredIntegerValue(attributes, COUNTRY_CODE_KEY);
        Long phoneNumber = DynamoAttributeValueUtils.getRequiredLongValue(attributes, PHONE_NUMBER_KEY);
        return new MobilePhone(countryCode, phoneNumber);
    }

    @Override
    public void createMobilePhoneForUserId(@Nullable MobilePhone mobilePhone, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (mobilePhone== null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createMobilePhoneForUserId, but received: mobilePhone=%s, userId=%s.", mobilePhone, userId));
        }
        createMobilePhoneForUserId(mobilePhone, userId, TimeUtils.getEpochTimeNowInUTC());
    } 

    @Override
    public void updateMobilePhoneForUserId(@Nullable MobilePhone curPhone, @Nullable MobilePhone newPhone, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        if (curPhone == null || newPhone == null || userId == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for updateMobilePhoneForUserId, but received: curPhone=%s, newPhone=%s, userId=%s.", 
                            curPhone, newPhone, userId));
        }
        
        LOG.error(curPhone);
        LOG.error(getMobilePhone(userId, false));
        if (!curPhone.equals(getMobilePhone(userId, false))) {
            throw new ItemNotFoundException();
        }
        Long now = TimeUtils.getEpochTimeNowInUTC();
        createMobilePhoneForUserId(newPhone, userId, now);
        deleteMobilePhoneForUserId(curPhone, userId);
    }

    @Override
    public @Nonnull Long getUserId(@Nullable MobilePhone mobilePhone) 
            throws ValidationException , ItemNotFoundException, RepositoryServerException {
        if (mobilePhone == null) {
            throw new ValidationException("Expecting non-null request paramter for getUserId, but received: mobilePhone=null.");
        }
        return getUserIdForMobilePhone(mobilePhone);
    }

    @Override
    public @Nonnull MobilePhone getMobilePhone(@Nullable Long userId, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException {
        if (userId == null) {
            throw new ValidationException("Expecting non-null request paramter for getMobilePhone, but received: userId=null.");
        }
        return queryMobilePhoneForUserId(userId, checkStaleness);
    }

    private @Nonnull Long getUserIdForMobilePhone(@Nonnull MobilePhone mobilePhone) throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PHONE_NUMBER_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getPhoneNumber()));
        key.put(COUNTRY_CODE_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getCountryCode()));

        GetItemRequest getItemRequest = new GetItemRequest().
                withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME).withKey(key).withAttributesToGet(USER_ID_KEY);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.getItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getUserIdForMobilePhone %s from table %s.", getItemRequest, MOBILE_PHONE_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.warn( String.format("The mobile phone %s in the getUserIdForMobilePhone request %s does not exist in the table.", mobilePhone, getItemRequest));
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(getItemResult.getItem(), USER_ID_KEY);

    }

    private void createMobilePhoneForUserId(@Nonnull MobilePhone mobilePhone, @Nonnull Long userId, @Nonnull Long activateTime) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(PHONE_NUMBER_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getPhoneNumber()));
        item.put(COUNTRY_CODE_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getCountryCode()));
        item.put(USER_ID_KEY, DynamoAttributeValueUtils.numberAttrValue(userId));
        item.put(ACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(activateTime));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(PHONE_NUMBER_KEY, DynamoAttributeValueUtils.expectEmpty());
        expected.put(COUNTRY_CODE_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().
                withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.warn( String.format("The mobile phone %s in createMobilePhoneForUserId request %s already existed.", mobilePhone, putItemRequest), error);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createMobilePhoneForUserId %s to table %s.", putItemRequest, MOBILE_PHONE_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteMobilePhoneForUserId(@Nonnull MobilePhone mobilePhone, @Nonnull Long userId) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(PHONE_NUMBER_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getPhoneNumber()));
        key.put(COUNTRY_CODE_KEY, DynamoAttributeValueUtils.numberAttrValue(mobilePhone.getCountryCode()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(USER_ID_KEY, DynamoAttributeValueUtils.expectEqual(DynamoAttributeValueUtils.numberAttrValue(userId)));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME).withKey(key).withExpected(expected);

        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException | ConditionalCheckFailedException error) {
            LOG.warn( String.format("The user id %s in deleteMobilePhoneForUserId request %s does not match with one in table.", userId, deleteItemRequest), error);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteMobilePhoneForUserId %s from table %s.", deleteItemRequest, MOBILE_PHONE_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private @Nonnull MobilePhone queryMobilePhoneForUserId(@Nonnull Long userId, boolean checkStaleness) 
            throws ItemNotFoundException, StaleDataException, RepositoryServerException {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(USER_ID_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(userId)));

        QueryRequest queryRequest = new QueryRequest().withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME).withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
                .withKeyConditions(keyConditions).withAttributesToGet(PHONE_NUMBER_KEY, COUNTRY_CODE_KEY).withScanIndexForward(false).withLimit(1);

        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to queryMobilePhoneForUserId %s from table %s.", queryRequest, MOBILE_PHONE_TO_ID_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.warn( String.format("The user id %s in the queryMobilePhoneForUserId request %s does not exist in the table.", userId, queryResult));
            throw new ItemNotFoundException();
        }
        MobilePhone mobilePhone = buildMobilePhone(queryResult.getItems().get(0));
        if (!checkStaleness) {
            return mobilePhone;
        }
        try {
            Long userIdForMobilePhone = getUserIdForMobilePhone(mobilePhone);
            if (userId.equals(userIdForMobilePhone)) {
                return mobilePhone;
            }
        } catch (ItemNotFoundException error) {}
        LOG.warn( String.format("Found stale mobile phone for user id %s", mobilePhone, userId));
        throw new StaleDataException();
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {

        GlobalSecondaryIndex userIdActivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(USER_ID_ACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(USER_ID_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );
        GlobalSecondaryIndex countryCodePhoneNumberGSI = new GlobalSecondaryIndex()
        .withIndexName(COUNTRY_CODE_PHONE_NUMBER_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
        .withKeySchema(
                new KeySchemaElement(COUNTRY_CODE_KEY, KeyType.HASH),
                new KeySchemaElement(PHONE_NUMBER_KEY, KeyType.RANGE)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(PHONE_NUMBER_KEY, ScalarAttributeType.N),
                new AttributeDefinition(COUNTRY_CODE_KEY, ScalarAttributeType.N),
                new AttributeDefinition(USER_ID_KEY, ScalarAttributeType.N),
                new AttributeDefinition(ACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N))
                .withKeySchema(new KeySchemaElement(PHONE_NUMBER_KEY, KeyType.HASH), 
                        new KeySchemaElement(COUNTRY_CODE_KEY, KeyType.RANGE))
                        .withGlobalSecondaryIndexes(userIdActivateInEpochGSI, countryCodePhoneNumberGSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", MOBILE_PHONE_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(MOBILE_PHONE_TO_ID_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", MOBILE_PHONE_TO_ID_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
