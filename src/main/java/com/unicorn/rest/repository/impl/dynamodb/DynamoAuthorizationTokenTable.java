package com.unicorn.rest.repository.impl.dynamodb;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;
import com.unicorn.rest.repository.table.AuthorizationTokenTable;
import com.unicorn.rest.utils.TimeUtils;

@Service
public class DynamoAuthorizationTokenTable implements AuthorizationTokenTable {
    private static final Logger LOG = LogManager.getLogger(DynamoAuthorizationTokenTable.class);

    private static final String AUTHORIZATION_TOKEN_TYPE_KEY = "AUTHORIZATION_TOKEN_TYPE"; //HashKey
    private static final String AUTHORIZATION_TOKEN_KEY = "AUTHORIZATION_TOKEN"; //RangeKey
    private static final String ISSUED_IN_EPOCH_KEY = "ISSUED_IN_EPOCH";
    private static final String EXPIRED_IN_EPOCH_KEY = "EXPIRED_IN_EPOCH";
    private static final String PRINCIPAL_KEY = "PRINCIPAL";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();

    @Override
    public void persistToken(AuthorizationToken authorizationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (authorizationToken == null) {
            throw new ValidationException("Expecting non-null request paramter for persistToken, but received: authorizationToken=null");
        }
        persistAuthorizationToken(authorizationToken);
    }

    @Override
    public void revokeTokenForPrincipal(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for revokeToken, but received: authorizationToken=%s, authorizationToken=%s, principal=%s", 
                            tokenType, token, principal));
        }
        revokeAuthorizationToken(tokenType, token, principal);
    }

    @Override
    public AuthorizationToken getToken(AuthorizationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getToken, but received: authorizationToken=%s, authorizationToken=%s", 
                            tokenType, token));
        }
        Map<String, AttributeValue> tokenAttrs = getAuthorizationToken(tokenType, token);
        return AuthorizationToken.buildTokenBuilder(token).tokenType(tokenType)
                .issuedAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, ISSUED_IN_EPOCH_KEY)))
                .expiredAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, EXPIRED_IN_EPOCH_KEY)))
                .principal(DynamoAttributeValueUtils.getLongValue(tokenAttrs, PRINCIPAL_KEY))
                .build();
    }

    @Override
    public AuthorizationToken getTokenForPrincipal(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getToken, but received: authorizationToken=%s, authorizationToken=%s, principal=%s", 
                            tokenType, token, principal));
        }
        Map<String, AttributeValue> tokenAttrs = getAuthorizationToken(tokenType, token);

        if (!principal.equals(DynamoAttributeValueUtils.getLongValue(tokenAttrs, PRINCIPAL_KEY))) {
            LOG.info("The token {} with token type {} for principal {} in getTokenForPrincipal request does not exist in the table.", token, tokenType.name(), principal);
            throw new ItemNotFoundException();
        }
        
        return AuthorizationToken.buildTokenBuilder(token).tokenType(tokenType)
                .issuedAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, ISSUED_IN_EPOCH_KEY)))
                .expiredAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, EXPIRED_IN_EPOCH_KEY)))
                .principal(principal)
                .build();
    }

    @Override
    public void deleteExpiredToken(AuthorizationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for deleteToken, but received: authorizationToken=%s, authorizationToken=%s", 
                            tokenType, token));
        }
        deleteExpiredAuthorizationToken(tokenType, token);
    }

    private Map<String, AttributeValue> getAuthorizationToken(@Nonnull AuthorizationTokenType tokenType, @Nonnull String token) 
            throws ItemNotFoundException, RepositoryServerException {
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHORIZATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHORIZATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        GetItemRequest getItemRequest = new GetItemRequest().withTableName(AUTHORIZATION_TOKEN_TABLE_NAME).withKey(key);
        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getAuthorizationToken %s from table %s.", getItemRequest, AUTHORIZATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The token {} with token type {} in the getAuthorizationToken request does not exist in the table.", 
                    token, tokenType.name());
            throw new ItemNotFoundException();
        }
        return getItemResult.getItem();
    }

    private void persistAuthorizationToken(@Nonnull AuthorizationToken authorizationToken) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(AUTHORIZATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(authorizationToken.getTokenType().name()));
        item.put(AUTHORIZATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(authorizationToken.getToken()));
        item.put(ISSUED_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(authorizationToken.getIssuedAt().getMillis()));
        item.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(authorizationToken.getExpireAt().getMillis()));
        item.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(authorizationToken.getPrincipal()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(AUTHORIZATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.expectEmpty());
        expected.put(AUTHORIZATION_TOKEN_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(AUTHORIZATION_TOKEN_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The token {} with token type {} in persistAuthorizationToken request already existed.", 
                    authorizationToken.getToken(), authorizationToken.getTokenType().name());
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to persistAuthorizationToken %s to table %s.", putItemRequest, AUTHORIZATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private void revokeAuthorizationToken(@Nonnull AuthorizationTokenType tokenType, @Nonnull String token, @Nonnull Long principal) 
            throws ItemNotFoundException, RepositoryServerException {
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHORIZATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHORIZATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        AttributeValue now = DynamoAttributeValueUtils.numberAttrValue(TimeUtils.getEpochTimeNowInUTC());
        Map<String, AttributeValueUpdate> updateItems = new HashMap<>();
        updateItems.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.updateTo(now));

        Map<String, ExpectedAttributeValue> expectedValues = new HashMap<>();
        AttributeValue principalAttrValue = DynamoAttributeValueUtils.numberAttrValue(principal);
        expectedValues.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEqual(principalAttrValue));
        expectedValues.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectCompare(ComparisonOperator.GT, now));
        
        UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(AUTHORIZATION_TOKEN_TABLE_NAME)
                .withKey(key).withAttributeUpdates(updateItems).withExpected(expectedValues);

        try {
            awsDynamoDBDAO.updateItem(updateItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The token {} with token type {} in revokeAuthorizationToken request does not exist or already expired in the table.", token, tokenType.name());
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to revokeAuthorizationToken %s to table %s.", updateItemRequest, AUTHORIZATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private void deleteExpiredAuthorizationToken(@Nonnull AuthorizationTokenType tokenType, @Nonnull String token) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHORIZATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHORIZATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        Map<String, ExpectedAttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectCompare(ComparisonOperator.LT, 
                DynamoAttributeValueUtils.numberAttrValue(TimeUtils.getEpochTimeNowInUTC())));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(AUTHORIZATION_TOKEN_TABLE_NAME).withKey(key);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException error) {
            LOG.info("The token {} with token type {} in deleteAuthorizationToken request does not match with one in table.", token, tokenType.name());
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteExpiredAuthorizationToken %s from table %s.", deleteItemRequest, AUTHORIZATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(AUTHORIZATION_TOKEN_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(8L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(AUTHORIZATION_TOKEN_TYPE_KEY, ScalarAttributeType.S),
                new AttributeDefinition(AUTHORIZATION_TOKEN_KEY, ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement(AUTHORIZATION_TOKEN_TYPE_KEY, KeyType.HASH),
                        new KeySchemaElement(AUTHORIZATION_TOKEN_KEY, KeyType.RANGE));
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", AUTHORIZATION_TOKEN_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }

    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(AUTHORIZATION_TOKEN_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", AUTHORIZATION_TOKEN_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
