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
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.repository.table.AuthenticationTokenTable;
import com.unicorn.rest.utils.TimeUtils;

@Service
public class DynamoAuthenticationTokenTable implements AuthenticationTokenTable {
    private static final Logger LOG = LogManager.getLogger(DynamoAuthenticationTokenTable.class);

    public static final String AUTHENTICATION_TOKEN_TABLE_NAME = "AUTHENTICATION_TOKEN_TABLE";
    public static final String AUTHENTICATION_TOKEN_TYPE_KEY = "AUTHENTICATION_TOKEN_TYPE"; //HashKey
    public static final String AUTHENTICATION_TOKEN_KEY = "AUTHENTICATION_TOKEN"; //RangeKey
    public static final String ISSUED_IN_EPOCH_KEY = "ISSUED_IN_EPOCH";
    public static final String EXPIRED_IN_EPOCH_KEY = "EXPIRED_IN_EPOCH";
    public static final String PRINCIPAL_KEY = "PRINCIPAL";
    public static final String SCOPE_KEY = "SCOPE";

    private final DynamoDBDAO awsDynamoDBDAO = DynamoDBDAO.get();

    @Override
    public void persistToken(AuthenticationToken authenticationToken) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        if (authenticationToken == null) {
            throw new ValidationException("Expecting non-null request paramter for persistToken, but received: authenticationToken=null");
        }
        persistAuthenticationToken(authenticationToken);
    }

    @Override
    public void revokeTokenForPrincipal(AuthenticationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for revokeToken, but received: authenticationToken=%s, authenticationToken=%s, principal=%s", 
                            tokenType, token, principal));
        }
        revokeAuthenticationToken(tokenType, token, principal);
    }

    @Override
    public AuthenticationToken getToken(AuthenticationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getToken, but received: authenticationToken=%s, authenticationToken=%s", 
                            tokenType, token));
        }
        Map<String, AttributeValue> tokenAttrs = getAuthenticationToken(tokenType, token);
        return AuthenticationToken.buildTokenBuilder(token).tokenType(tokenType)
                .issuedAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, ISSUED_IN_EPOCH_KEY)))
                .expiredAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, EXPIRED_IN_EPOCH_KEY)))
                .principal(DynamoAttributeValueUtils.getLongValue(tokenAttrs, PRINCIPAL_KEY))
                .build();
    }

    @Override
    public AuthenticationToken getTokenForPrincipal(AuthenticationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null || principal == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for getToken, but received: authenticationToken=%s, authenticationToken=%s, principal=%s", 
                            tokenType, token, principal));
        }
        Map<String, AttributeValue> tokenAttrs = getAuthenticationToken(tokenType, token);

        if (!principal.equals(DynamoAttributeValueUtils.getLongValue(tokenAttrs, PRINCIPAL_KEY))) {
            LOG.info("The token {} with token type {} for principal {} in getTokenForPrincipal request does not exist in the table.", token, tokenType.name(), principal);
            throw new ItemNotFoundException();
        }
        
        return AuthenticationToken.buildTokenBuilder(token).tokenType(tokenType)
                .issuedAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, ISSUED_IN_EPOCH_KEY)))
                .expiredAt(TimeUtils.convertToDateTimeInUTCWithEpochTime(DynamoAttributeValueUtils.getRequiredLongValue(tokenAttrs, EXPIRED_IN_EPOCH_KEY)))
                .principal(principal)
                .build();
    }

    @Override
    public void deleteExpiredToken(AuthenticationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (tokenType == null || token == null) {
            throw new ValidationException(
                    String.format("Expecting non-null request paramter for deleteToken, but received: authenticationToken=%s, authenticationToken=%s", 
                            tokenType, token));
        }
        deleteExpiredAuthenticationToken(tokenType, token);
    }

    private Map<String, AttributeValue> getAuthenticationToken(@Nonnull AuthenticationTokenType tokenType, @Nonnull String token) 
            throws ItemNotFoundException, RepositoryServerException {
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHENTICATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHENTICATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        GetItemRequest getItemRequest = new GetItemRequest().withTableName(AUTHENTICATION_TOKEN_TABLE_NAME).withKey(key);
        GetItemResult getItemResult;
        try {
            getItemResult = awsDynamoDBDAO.consistentGetItem(getItemRequest);
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to getAuthenticationToken %s from table %s.", getItemRequest, AUTHENTICATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
        if (CollectionUtils.sizeIsEmpty(getItemResult.getItem())) {
            LOG.info("The token {} with token type {} in the getAuthenticationToken request does not exist in the table.", 
                    token, tokenType.name());
            throw new ItemNotFoundException();
        }
        return getItemResult.getItem();
    }

    private void persistAuthenticationToken(@Nonnull AuthenticationToken authenticationToken) 
            throws DuplicateKeyException, RepositoryServerException {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(AUTHENTICATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(authenticationToken.getTokenType().name()));
        item.put(AUTHENTICATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(authenticationToken.getToken()));
        item.put(ISSUED_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(authenticationToken.getIssuedAt().getMillis()));
        item.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.numberAttrValue(authenticationToken.getExpireAt().getMillis()));
        item.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.numberAttrValue(authenticationToken.getPrincipal()));

        Map<String, ExpectedAttributeValue> expected = new HashMap<>();
        expected.put(AUTHENTICATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.expectEmpty());
        expected.put(AUTHENTICATION_TOKEN_KEY, DynamoAttributeValueUtils.expectEmpty());

        PutItemRequest putItemRequest = new PutItemRequest().withTableName(AUTHENTICATION_TOKEN_TABLE_NAME).withItem(item).withExpected(expected);

        try {
            awsDynamoDBDAO.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The token {} with token type {} in persistAuthenticationToken request already existed.", 
                    authenticationToken.getToken(), authenticationToken.getTokenType().name());
            throw new DuplicateKeyException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to persistAuthenticationToken %s to table %s.", putItemRequest, AUTHENTICATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private void revokeAuthenticationToken(@Nonnull AuthenticationTokenType tokenType, @Nonnull String token, @Nonnull Long principal) 
            throws ItemNotFoundException, RepositoryServerException {
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHENTICATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHENTICATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        AttributeValue now = DynamoAttributeValueUtils.numberAttrValue(TimeUtils.getEpochTimeNowInUTC());
        Map<String, AttributeValueUpdate> updateItems = new HashMap<>();
        updateItems.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.updateTo(now));

        Map<String, ExpectedAttributeValue> expectedValues = new HashMap<>();
        AttributeValue principalAttrValue = DynamoAttributeValueUtils.numberAttrValue(principal);
        expectedValues.put(PRINCIPAL_KEY, DynamoAttributeValueUtils.expectEqual(principalAttrValue));
        expectedValues.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectCompare(ComparisonOperator.GT, now));
        
        UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(AUTHENTICATION_TOKEN_TABLE_NAME)
                .withKey(key).withAttributeUpdates(updateItems).withExpected(expectedValues);

        try {
            awsDynamoDBDAO.updateItem(updateItemRequest);
        } catch (ConditionalCheckFailedException error) {
            LOG.info("The token {} with token type {} in revokeAuthenticationToken request does not exist or already expired in the table.", token, tokenType.name());
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to revokeAuthenticationToken %s to table %s.", updateItemRequest, AUTHENTICATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    private void deleteExpiredAuthenticationToken(@Nonnull AuthenticationTokenType tokenType, @Nonnull String token) 
            throws ItemNotFoundException, RepositoryServerException{
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(AUTHENTICATION_TOKEN_TYPE_KEY, DynamoAttributeValueUtils.stringAttrValue(tokenType.name()));
        key.put(AUTHENTICATION_TOKEN_KEY, DynamoAttributeValueUtils.stringAttrValue(token));

        Map<String, ExpectedAttributeValue> expectedValues = new HashMap<>();
        expectedValues.put(EXPIRED_IN_EPOCH_KEY, DynamoAttributeValueUtils.expectCompare(ComparisonOperator.LT, 
                DynamoAttributeValueUtils.numberAttrValue(TimeUtils.getEpochTimeNowInUTC())));

        DeleteItemRequest deleteItemRequest = new DeleteItemRequest().
                withTableName(AUTHENTICATION_TOKEN_TABLE_NAME).withKey(key);
        try {
            awsDynamoDBDAO.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException error) {
            LOG.info("The token {} with token type {} in deleteAuthenticationToken request does not match with one in table.", token, tokenType.name());
            throw new ItemNotFoundException();
        } catch (AmazonClientException error) {
            LOG.error( String.format("Failed while attempting to deleteUserNameForUserId %s from table %s.", deleteItemRequest, AUTHENTICATION_TOKEN_TABLE_NAME), error);
            throw new RepositoryServerException(error);
        }
    }

    public void createTable() 
            throws RepositoryClientException, RepositoryServerException {
        CreateTableRequest createTableRequest = new CreateTableRequest()
        .withTableName(AUTHENTICATION_TOKEN_TABLE_NAME)
        .withProvisionedThroughput(new ProvisionedThroughput(8L, 1L))
        .withAttributeDefinitions(
                new AttributeDefinition(AUTHENTICATION_TOKEN_TYPE_KEY, ScalarAttributeType.S),
                new AttributeDefinition(AUTHENTICATION_TOKEN_KEY, ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement(AUTHENTICATION_TOKEN_TYPE_KEY, KeyType.HASH),
                        new KeySchemaElement(AUTHENTICATION_TOKEN_KEY, KeyType.RANGE));
        try {
            awsDynamoDBDAO.createTable(createTableRequest);
        } catch (ResourceInUseException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to create already exists", AUTHENTICATION_TOKEN_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }

    }

    public void deleteTable() 
            throws RepositoryClientException, RepositoryServerException {
        try {
            awsDynamoDBDAO.deleteTable(new DeleteTableRequest().withTableName(AUTHENTICATION_TOKEN_TABLE_NAME));
        } catch (ResourceNotFoundException error) {
            throw new RepositoryClientException(String.format("Table %s attempted to delete does not exist", AUTHENTICATION_TOKEN_TABLE_NAME));
        } catch (AmazonClientException error) {
            throw new RepositoryServerException(error);
        }
    }
}
