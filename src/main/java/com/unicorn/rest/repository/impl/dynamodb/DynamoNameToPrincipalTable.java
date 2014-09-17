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
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.repository.table.NameToPrincipalTable;
import com.unicorn.rest.utils.TimeUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Service
public class DynamoNameToPrincipalTable implements NameToPrincipalTable {
    private static final Logger LOG = LogManager.getLogger(DynamoNameToPrincipalTable.class);

    private static final String NAME_KEY = "NAME"; //HashKey
    private static final String PRINCIPAL_KEY = "PRINCIPAL";
    private static final String ACTIVATE_IN_EPOCH_KEY = "ACTIVATE_IN_EPOCH"; 
    private static final String DEACTIVATE_IN_EPOCH_KEY = "DEACTIVATE_IN_EPOCH"; //RangeKey

    private static final String ACTIVATE_IN_EPOCH_LSI_KEY = "ACTIVATE_IN_EPOCH-LSI";
    private static final String PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY = "PRINCIPAL-ACTIVATE_IN_EPOCH-GSI";
    private static final String PRINCIPAL_DEACTIVATE_IN_EPOCH_GSI_KEY = "PRINCIPAL-DEACTIVATE_IN_EPOCH-GSI";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();
    
    @EqualsAndHashCode()
    @ToString()
    @Getter
    public static class NameToPrincipalItem {

        @Nonnull private final String name;
        @Nonnull private final Long principal;
        private final Long activateTime;
        @Nonnull private final Long deactivateTime;

        public NameToPrincipalItem(@Nonnull String name, @Nonnull Long principal, @Nullable Long activateTime, @Nonnull Long deactivateTime) {
            this.name = name;
            this.principal = principal;
            this.activateTime = activateTime;
            this.deactivateTime = deactivateTime;
        }
        
        public static NameToPrincipalItem buildNameToPrincipalItem(@Nonnull Map<String, AttributeValue> attributes) throws RepositoryServerException {
            return new NameToPrincipalItem(DynamoAttributeValueUtils.getRequiredStringValue(attributes, NAME_KEY), 
                    DynamoAttributeValueUtils.getRequiredLongValue(attributes, PRINCIPAL_KEY),
                    DynamoAttributeValueUtils.getRequiredLongValue(attributes, ACTIVATE_IN_EPOCH_KEY),
                    DynamoAttributeValueUtils.getRequiredLongValue(attributes, DEACTIVATE_IN_EPOCH_KEY));
        }
    }

    @Override
    public void createNameForPrincipal(@Nullable Name name, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (name == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for createNameForPrincipal, but received: name=%s, principal=%s", name, principal));
        }
        createNameForPrincipal(new NameToPrincipalItem(name.getName(), principal, TimeUtils.getEpochTimeNowInUTC(), Long.MAX_VALUE));
    }

    @Override
    public void updateNameForPrincipal(@Nullable Name curName, @Nullable Name newName, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        if (curName == null || newName == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for updateNameForPrincipal, but received: curName=%s, newName=%s, principal=%s",
                    curName, newName, principal));
        } 
        String curNameStr = curName.getName();
        String newNameStr = newName.getName();
        
        NameToPrincipalItem curNameToPrincipalItem = queryNameForPrincipal(principal, false);
        if (!curNameStr.equals(curNameToPrincipalItem.getName())) {
            throw new ItemNotFoundException();
        };

        Long now = TimeUtils.getEpochTimeNowInUTC();
        createNameForPrincipal(new NameToPrincipalItem(newNameStr, principal, now, Long.MAX_VALUE));
        deactivateNameForPrincipal(curNameStr, principal, curNameToPrincipalItem.getActivateTime(), now);
    }

    @Override
    public @Nonnull Long getCurrentPrincipal(@Nullable Name name) 
            throws ValidationException , ItemNotFoundException, RepositoryServerException {
        if (name == null) {
            throw new ValidationException("Expecting non-null request paramter for getCurrentPrincipal, but received: name=null");
        }
        return getCurrentPrincipalForName(name.getName());
    }

    @Override
    public @Nonnull Long getPrincipalAtTime(@Nullable Name name, @Nullable Long activeTime) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (name == null || activeTime == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getPrincipalAtTime, but received: name=%s, activeTime=%s", name, activeTime));
        }
        return getPrincipalForNameAtTime(name.getName(), activeTime);
    }

    @Override
    public @Nonnull String getName(@Nullable Long principal, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException {
        if (principal == null) {
            throw new ValidationException("Expecting non-null request paramter for getName, but received: principal=null");
        }
        return queryNameForPrincipal(principal, checkStaleness).getName();
    }
    
    /**
     * 
     * Deactivate name_ame for principal
     * Create the current name to principal with correct deactivate_time to name_to_principal_table
     * and delete the name to principal with deactivate_time as Long.MAX_VALUE
     * 
     * @Note:
     * If the first step succeeds and the second steps failed, there is a 
     * chance that user/customer will have multiple names to log in. 
     * However, we can still figure out which one is the latest by checking the activate_in_epoch 
     * 
     * @param curName
     * @param principal
     * @param deactivateTime
     * @throws DuplicateKeyException if request is invalid
     * @throws ItemNotFoundException 
     * @throws RepositoryServerException internal server error
     */
    private void deactivateNameForPrincipal(@Nonnull String curName, @Nonnull Long principal,  @Nonnull Long curActivateTime, @Nonnull Long deactivateTime) 
            throws DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        createNameForPrincipal(new NameToPrincipalItem(curName, principal, curActivateTime, deactivateTime));
        deleteNameForPrincipal(new NameToPrincipalItem(curName, principal, curActivateTime, Long.MAX_VALUE));
    }

    private @Nonnull Long getCurrentPrincipalForName(@Nonnull String name) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(name));
        key.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(Long.MAX_VALUE));

        GetItemRequest getItemRequest = new GetItemRequest().
                withTableName(NAME_TO_PRINCIPAL_TABLE_NAME).withKey(key).withAttributesToGet(PRINCIPAL_KEY);

        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getCurrentPrincipalForName %s from table %s.", getItemRequest, NAME_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The name {} in the getCurrentPrincipalForName request does not exist in the table.", name);
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(getItemResult.getItem(), PRINCIPAL_KEY);
    }

    private @Nonnull Long getPrincipalForNameAtTime(@Nonnull String name, @Nonnull Long activeTime) 
            throws ItemNotFoundException, RepositoryServerException {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(NAME_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.stringAttrValue(name)));
        keyConditions.put(ACTIVATE_IN_EPOCH_KEY, new Condition().withComparisonOperator(ComparisonOperator.LT)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(activeTime)));

        QueryRequest queryRequest = new QueryRequest().withTableName(NAME_TO_PRINCIPAL_TABLE_NAME).withIndexName(ACTIVATE_IN_EPOCH_LSI_KEY)
                .withKeyConditions(keyConditions).withScanIndexForward(false).withLimit(1);
        
        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getPrincipalForNameAtTime %s from table %s.", queryRequest, NAME_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.info("The name {} with active time {} in the getPrincipalForNameAtTime request does not exist in the table.", 
                    name, activeTime);
            throw new ItemNotFoundException();
        }
        if (activeTime > DynamoAttributeValueUtils.getRequiredLongValue(queryResult.getItems().get(0), DEACTIVATE_IN_EPOCH_KEY)) {
            LOG.info("The name {} with active time {} in the getPrincipalForNameAtTime request does not exist in the table.", 
                    name, activeTime);
            throw new ItemNotFoundException();
        }
        return DynamoAttributeValueUtils.getRequiredLongValue(queryResult.getItems().get(0), PRINCIPAL_KEY);
    }

    private void createNameForPrincipal(@Nonnull NameToPrincipalItem nameToPrincipalItem) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(nameToPrincipalItem.getName()));
        item.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(nameToPrincipalItem.getPrincipal()));
        item.put(ACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(nameToPrincipalItem.getActivateTime()));
        item.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(nameToPrincipalItem.getDeactivateTime()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(NAME_KEY, DynamoAttributeValueUtils.expectEmpty());
        expected.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(NAME_TO_PRINCIPAL_TABLE_NAME).withItem(item).withExpected(expected);
        
        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The name {} in createNameForPrincipal request already existed.", nameToPrincipalItem.getName());
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to createNameForPrincipal %s to table %s.", putItemRequest, NAME_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected void deleteNameForPrincipal(@Nonnull NameToPrincipalItem nameToPrincipalItem) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(NAME_KEY, DynamoAttributeValueUtils.stringAttrValue(nameToPrincipalItem.getName()));
        key.put(DEACTIVATE_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(nameToPrincipalItem.getDeactivateTime()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEqual(DynamoAttributeValueUtils.numberAttrValue(nameToPrincipalItem.getPrincipal())));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(NAME_TO_PRINCIPAL_TABLE_NAME).withKey(key).withExpected(expected);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException | ConditionalCheckFailedException error) {
            LOG.info("The principal {} in deleteNameForPrincipal request does not match with one in table.", nameToPrincipalItem.getPrincipal());
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteNameForPrincipal %s from table %s.", deleteItemRequest, NAME_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    /*
     * This method is protected for unit test
     */
    protected @Nonnull NameToPrincipalItem queryNameForPrincipal(@Nonnull Long principal, boolean checkStaleness) 
            throws ItemNotFoundException, StaleDataException, RepositoryServerException {
        Map<String, Condition> keyConditions = new HashMap<>();
        keyConditions.put(PRINCIPAL_KEY, new Condition().withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(DynamoAttributeValueUtils.numberAttrValue(principal)));

        QueryRequest queryRequest = new QueryRequest().withTableName(NAME_TO_PRINCIPAL_TABLE_NAME).withIndexName(PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY)
                .withKeyConditions(keyConditions).withScanIndexForward(false).withLimit(1);
        
        QueryResult queryResult;
        try {
            queryResult = awsDynamoDBDAO.queryOnce(queryRequest);
        } catch(AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to queryNameForPrincipal %s from table %s.", queryRequest, NAME_TO_PRINCIPAL_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(queryResult.getItems())) {
            LOG.info("The principal {} in the queryNameForPrincipal request does not exist in the table.", principal);
            throw new ItemNotFoundException();
        }

        NameToPrincipalItem nameToPrincipalItem = NameToPrincipalItem.buildNameToPrincipalItem(queryResult.getItems().get(0));
        if (!checkStaleness) {
            return nameToPrincipalItem;
        }

        String name = nameToPrincipalItem.getName();
        try {
            Long principalForName = getCurrentPrincipalForName(name);
            if (principal.equals(principalForName)) {
                return nameToPrincipalItem;
            }
        } catch (ItemNotFoundException error) {}
        LOG.warn("Found stale name {} for principal {}.", name, principal);
        throw new StaleDataException();
    }
    
    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        
        LocalSecondaryIndex activateInEpochLSI = new LocalSecondaryIndex()
        .withIndexName(ACTIVATE_IN_EPOCH_LSI_KEY)
        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
        .withKeySchema(
                new KeySchemaElement(NAME_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );
        
        GlobalSecondaryIndex principalActivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(PRINCIPAL_ACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(PRINCIPAL_KEY, KeyType.HASH),
                new KeySchemaElement(ACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );
        
        GlobalSecondaryIndex principalDeactivateInEpochGSI = new GlobalSecondaryIndex()
        .withIndexName(PRINCIPAL_DEACTIVATE_IN_EPOCH_GSI_KEY)
        .withProvisionedThroughput(new ProvisionedThroughput(2L, 1L))
        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY))
        .withKeySchema(
                new KeySchemaElement(PRINCIPAL_KEY, KeyType.HASH),
                new KeySchemaElement(DEACTIVATE_IN_EPOCH_KEY, KeyType.RANGE)
                );

        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(NAME_TO_PRINCIPAL_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(4L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(NAME_KEY, ScalarAttributeType.S),
                new AttributeDefinition(PRINCIPAL_KEY, ScalarAttributeType.N),
                new AttributeDefinition(ACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N),
                new AttributeDefinition(DEACTIVATE_IN_EPOCH_KEY, ScalarAttributeType.N))
                .withKeySchema(new KeySchemaElement(NAME_KEY, KeyType.HASH), 
                        new KeySchemaElement(DEACTIVATE_IN_EPOCH_KEY, KeyType.RANGE))
                .withGlobalSecondaryIndexes(principalActivateInEpochGSI, principalDeactivateInEpochGSI)
                .withLocalSecondaryIndexes(activateInEpochLSI);
        
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", NAME_TO_PRINCIPAL_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(NAME_TO_PRINCIPAL_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", NAME_TO_PRINCIPAL_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
