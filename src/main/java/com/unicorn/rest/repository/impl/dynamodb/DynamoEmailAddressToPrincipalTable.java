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
import com.unicorn.rest.repository.table.EmailAddressToPrincipalTable;
import com.unicorn.rest.utils.TimeUtils;

@Service
public class DynamoEmailAddressToPrincipalTable implements EmailAddressToPrincipalTable {
    private static final Logger LOG = LogManager.getLogger(DynamoEmailAddressToPrincipalTable.class);

    private static final String EMAIL_ADDRESS_KEY = "EMAIL_ADDRESS"; //HashKey
    private static final String PRINCIPAL_KEY = "PRINCIPAL";
    private static final String ACTIVATE_IN_EPOCH_KEY = "ACTIVATE_IN_EPOCH"; 

    private static final String PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY = "PRINCIPAL-ACTIVATE_IN_EPOCH-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();
    
    @Override
    public void createEmailAddressForPrincipal(@Nullable EmailAddress emailAddress, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (emailAddress == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createEmailAddressForPrincipal, but received: emailAddress=%s, principal=%s", 
                            emailAddress, principal));
        }
        createEmailAddressForPrincipal(emailAddress.getEmailAddress(), principal, TimeUtils.getEpochTimeNowInUTC());
    } 

    @Override
    public void updateEmailAddressForPrincipal(@Nullable EmailAddress curEmailAddress, @Nullable EmailAddress newEmailAddress, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        if (curEmailAddress == null|| newEmailAddress == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for updateEmailAddressForPrincipal, but received: curEmailAddress=%s, newEmailAddress=%s, principal=%s", 
                            curEmailAddress, newEmailAddress, principal));
        }
        
        String curEmailAddressStr = curEmailAddress.getEmailAddress();
        String newEmailAddressStr = newEmailAddress.getEmailAddress();
        
        if (!curEmailAddressStr.equals(getEmailAddress(principal, false))) {
            throw new ItemNotFoundException();
        };
        Long now = TimeUtils.getEpochTimeNowInUTC();
        createEmailAddressForPrincipal(newEmailAddressStr, principal, now);
        deleteEmailAddressForPrincipal(curEmailAddressStr, principal);
    }

    @Override
    public @Nonnull Long getPrincipal(@Nullable EmailAddress emailAddress) 
            throws ValidationException , ItemNotFoundException, RepositoryServerException {
        if (emailAddress == null) {
            throw new ValidationException("Expecting non-null request paramter for getPrincipal, but received: emailAddress=null");
        }
        return getPrincipalForEmailAddress(emailAddress.getEmailAddress());
    }

    @Override
    public @Nonnull String getEmailAddress(@Nullable Long principal, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException {
        if (principal == null) {
            throw new ValidationException("Expecting non-null request paramter for getEmailAddress, but received: principal=null");
        }
        return queryEmailAddressForPrincipal(principal, checkStaleness);
    }

    private @Nonnull Long getPrincipalForEmailAddress(@Nonnull String emailAddress) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));

        GetItemRequest getItemRequest = new GetItemRequest().
                withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME).withKey(key).withAttributesToGet(PRINCIPAL_KEY);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getPrincipalForEmailAddress %s from table %s.", getItemRequest, EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The email address {} in the getPrincipalForEmailAddress request does not exist in the table.", emailAddress);
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(getItemResult.getItem(), PRINCIPAL_KEY);

    }

    private void createEmailAddressForPrincipal(@Nonnull String emailAddress, @Nonnull Long principal, @Nonnull Long activateTime) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));
        item.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(principal));
        item.put(ACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(activateTime));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().
                withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The email address {} in createEmailAddressForPrincipal request already existed.", emailAddress);
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createEmailAddressForPrincipal %s to table %s.", putItemRequest, EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteEmailAddressForPrincipal(@Nonnull String emailAddress, @Nonnull Long principal) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(EMAIL_ADDRESS_KEY, DynamoAttributeValueUtils.stringAttrValue(emailAddress));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEqual(DynamoAttributeValueUtils.numberAttrValue(principal)));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME).withKey(key).withExpected(expected);

        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException | ConditionalCheckFailedException error) {
            LOG.info("The principal {} in deleteEmailAddressForPrincipal request does not match with one in table.", principal);
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteEmailAddressForPrincipal %s from table %s.", deleteItemRequest, EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private @Nonnull String queryEmailAddressForPrincipal(@Nonnull Long principal, boolean checkStaleness) 
            throws ItemNotFoundException, StaleDataException, RepositoryServerException {
        HashMap<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(PRINCIPAL_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(principal)));

        QueryRequest queryRequest = new QueryRequest().withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME).withIndexName(PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY)
                .withKeyConditions(keyConditions).withAttributesToGet(EMAIL_ADDRESS_KEY).withScanIndexForward(false).withLimit(1);

        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to queryEmailAddressForPrincipal %s from table %s.", queryRequest, EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.info("The principal {} in the queryEmailAddressForPrincipal request does not exist in the table.", principal);
            throw new ItemNotFoundException();
        }
        String emailAddress = DynamoAttributeValueUtils.getRequiredStringValue(queryResult.getItems().get(0), EMAIL_ADDRESS_KEY);
        if (!checkStaleness) {
            return emailAddress;
        }
        try {
            Long principalForEmailAddress = getPrincipalForEmailAddress(emailAddress);
            if (principal.equals(principalForEmailAddress)) {
                return emailAddress;
            }
        } catch (ItemNotFoundException error) {}
        LOG.warn("Found stale email address {} for principal {}.", emailAddress, principal);
        throw new StaleDataException();
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        
        GlobalSecondaryIndex principalActivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(PRINCIPAL_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(EMAIL_ADDRESS_KEY, ScalarAttributeType.S),
                new AttributeDefinition(PRINCIPAL_KEY, ScalarAttributeType.N),
                new AttributeDefinition(ACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N))
                .withKeySchema(new KeySchemaElement(EMAIL_ADDRESS_KEY, KeyType.HASH))
                .withGlobalSecondaryIndexes(principalActivateInEpochGSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", EMAIL_ADDRESS_TO_PRINCIPAL_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
