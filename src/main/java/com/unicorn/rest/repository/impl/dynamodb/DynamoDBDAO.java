package com.unicorn.rest.repository.impl.dynamodb;

import java.io.Closeable;
import java.io.IOException;

import lombok.Data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.unicorn.rest.server.PropertiesParser;

@Data
public class DynamoDBDAO implements Closeable {
    private static final Logger LOG = LogManager.getLogger(DynamoDBDAO.class);

    private static final String DYNAMODB_CREDENTIALS_FILE = "dynamodb.credentials";
    public static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY";
    public static final String AWS_SECRET_KEY = "AWS_SECRET_KEY";
    public static final String AWS_REGION = "AWS_REGION";

    private static final int MAX_NUM_OF_ATTEMPTS = 3;
    private static final int SLEEP_IN_MILLS_BETWEEN_ATTEMPS = 100;

    private final AmazonDynamoDBClient dynamoDBClient;

    private static DynamoDBDAO instance;
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (instance != null) {
                    try {
                        instance.close();
                        instance = null;
                    } catch (Exception error) {
                        LOG.error("Failed to close AWSDynamoDBDAO", error);
                    }
                }
            }
        }, "AWSDynamoDBDAO-ShutdownHook"));
    }

    public static synchronized DynamoDBDAO get() {
        if (instance != null) {
            return instance;
        }

        try {
            PropertiesParser dynamodbCredentialsParser = new PropertiesParser(DYNAMODB_CREDENTIALS_FILE);
            String accessKey = dynamodbCredentialsParser.getProperty(AWS_ACCESS_KEY);
            String accessSecretKey = dynamodbCredentialsParser.getProperty(AWS_SECRET_KEY);
            String region = dynamodbCredentialsParser.getProperty(AWS_REGION);

            AWSCredentials awsCredential = new BasicAWSCredentials(accessKey, accessSecretKey);
            AmazonDynamoDBClient client = new AmazonDynamoDBClient(new StaticCredentialsProvider(awsCredential));
            client.setRegion(Region.getRegion(Regions.fromName(region)));

            return instance = new DynamoDBDAO(client);
        } catch (Exception error) {
            throw new RuntimeException("Failed while attempting to initialize AWSDynamoDBDAO", error);
        }
    }

    public GetItemResult getItem(GetItemRequest getItemRequest) throws AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to get item %s from dynamodb.", getItemRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.getItem(getItemRequest);
            } catch (AmazonClientException error) { 
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {}
        }
    }

    public PutItemResult putItem(PutItemRequest putItemRequest) throws AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to put item %s to dynamodb.", putItemRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.putItem(putItemRequest);
            } catch (ConditionalCheckFailedException error) {
                throw error;
            } catch (AmazonClientException error) { 
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {}
        }
    }

    public UpdateItemResult updateItem(UpdateItemRequest updateItemRequest) throws AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to update item %s to dynamodb.", updateItemRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.updateItem(updateItemRequest);
            } catch (ConditionalCheckFailedException error) {
                throw error;
            } catch (AmazonClientException error) { 
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {}
        }
    }

    public DeleteItemResult deleteItem(DeleteItemRequest deleteItemRequest) throws AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to delete item %s from dynamodb", deleteItemRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.deleteItem(deleteItemRequest);
            } catch (ConditionalCheckFailedException error) {
                throw error;
            } catch (AmazonClientException error) { 
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {}
        }
    }

    public QueryResult queryOnce(QueryRequest queryRequest) throws AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to query from dynamodb with query request %s", queryRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.query(queryRequest);
            } catch (AmazonClientException error) { 
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {}
        }
    }

    public CreateTableResult createTable(CreateTableRequest createTableRequest) 
            throws ResourceInUseException, AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to create table in dynamodb with create request %s", createTableRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                CreateTableResult createTableResult = dynamoDBClient.createTable(createTableRequest);
                waitForTableToBecomeAvailable(createTableRequest.getTableName(), 5);

                return createTableResult;
            } catch (ResourceInUseException error) {
                throw error;
            } catch (AmazonClientException error) {
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {} 
        }
    }

    public DeleteTableResult deleteTable(DeleteTableRequest deleteTableRequest)
            throws ResourceNotFoundException, AmazonServiceException, AmazonClientException {
        LOG.debug( String.format("Attempting to delete table in dynamodb with delete request %s", deleteTableRequest));
        int numOfAttempts = 1;
        while (true) {
            try {
                DeleteTableResult deleteTableResult = dynamoDBClient.deleteTable(deleteTableRequest);
                waitForTableToBeDeleted(deleteTableRequest.getTableName(), 5);

                return deleteTableResult;
            } catch (ResourceNotFoundException error) {
                throw error;
            } catch (AmazonClientException error) {
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {} 
        }
    }

    public TableDescription getTableInformation(String tableName) 
            throws ResourceNotFoundException, AmazonServiceException, AmazonClientException {
        int numOfAttempts = 1;
        while (true) {
            try {
                return dynamoDBClient.describeTable(
                        new DescribeTableRequest().withTableName(tableName)).getTable();
            } catch (ResourceNotFoundException error) {
                throw error;
            } catch (AmazonClientException error) {
                if (numOfAttempts++ >= MAX_NUM_OF_ATTEMPTS || !error.isRetryable()) {
                    throw error;
                }
            }
            try {
                Thread.sleep(SLEEP_IN_MILLS_BETWEEN_ATTEMPS);
            } catch (InterruptedException ignore) {} 
        }
    }

    public void waitForTableToBecomeAvailable(String tableName, int waitingMinutes) {
        LOG.debug(String.format("Attempting to wait for table %s to become available", tableName));

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (waitingMinutes * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                TableDescription tableDescription = getTableInformation(tableName);
                String tableStatus = tableDescription.getTableStatus();
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) {
                    LOG.debug(String.format("Table %s become available after waiting for %s seconds", tableName, 
                            (System.currentTimeMillis() - startTime)/1000));
                    return;
                }
            } catch (AmazonClientException ignore) {}
            
            try { 
                Thread.sleep(1000 * 30); 
            } catch (Exception irgonre) {}
        }
        throw new RuntimeException("Table " + tableName + " never went active");
    }

    public void waitForTableToBeDeleted(String tableName, int waitingMinutes) {
        LOG.debug(String.format("Attempting to wait for table %s to be deleted", tableName));

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (waitingMinutes * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
                getTableInformation(tableName);
            } catch (ResourceNotFoundException error) {
                LOG.debug(String.format("Table %s is deleted after waiting for %s seconds", tableName, 
                        (System.currentTimeMillis() - startTime)/1000));
                return;
            } catch (AmazonClientException ignore) {}
            
            try {
                Thread.sleep(1000 * 30);
            } catch (Exception ignore) {}
        }
        throw new RuntimeException("Table " + tableName + " was never deleted");
    }

    @Override
    public void close() throws IOException {
        dynamoDBClient.shutdown();
    }
}