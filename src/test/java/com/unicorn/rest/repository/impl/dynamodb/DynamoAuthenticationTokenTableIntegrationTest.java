package com.unicorn.rest.repository.impl.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryClientException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthenticationTokenTable;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;
import com.unicorn.rest.utils.TimeUtils;

public class DynamoAuthenticationTokenTableIntegrationTest {
    private static DynamoAuthenticationTokenTable authenticationTokenTable;

    @BeforeClass
    public static void setUpAuthenticationTokenTable() throws RepositoryClientException, RepositoryServerException {
        authenticationTokenTable = new DynamoAuthenticationTokenTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        authenticationTokenTable.createTable();
    }
    
    @Test
    public void testPersistTokenHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(userId);
        try {
            authenticationTokenTable.persistToken(authenticationToken);
            AuthenticationToken persistedAuthenticationToken = authenticationTokenTable.getToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            assertEquals(authenticationToken, persistedAuthenticationToken);

        } finally {
            try {
                authenticationTokenTable.deleteExpiredToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testPersistTokenWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            authenticationTokenTable.persistToken(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testPersistTokenWithInvalidRequest");
    }
    
    @Test
    public void testPersistTokenWithExistedToken() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(userId);
        
        AuthenticationToken anotherAuthenticationToken = AuthenticationToken.buildTokenBuilder(authenticationToken.getToken()).
                tokenType(authenticationToken.getTokenType()).userId(SimpleFlakeKeyGenerator.generateKey()).build();
        try {
            authenticationTokenTable.persistToken(authenticationToken);
            try {
                authenticationTokenTable.persistToken(anotherAuthenticationToken);
            } catch(DuplicateKeyException error) { 
                AuthenticationToken persistedAuthenticationToken = authenticationTokenTable.getToken(authenticationToken.getTokenType(), authenticationToken.getToken());
                assertEquals(authenticationToken, persistedAuthenticationToken);
                return;
            }
            fail("Failed while running testPersistTokenWithExistedToken");
        } finally {
            try {
                authenticationTokenTable.deleteExpiredToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testRevokeTokenHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(userId);
        try {
            authenticationTokenTable.persistToken(authenticationToken);
            Long beforeRevoke = TimeUtils.getEpochTimeNowInUTC();
            Thread.sleep(100);
            authenticationTokenTable.revokeToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            Thread.sleep(100);
            Long afterRevoke = TimeUtils.getEpochTimeNowInUTC();
            AuthenticationToken persistedAuthenticationToken = authenticationTokenTable.getToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            assertEquals(authenticationToken.getTokenType(), persistedAuthenticationToken.getTokenType());
            assertEquals(authenticationToken.getToken(), persistedAuthenticationToken.getToken());
            assertEquals(authenticationToken.getIssuedAt(), persistedAuthenticationToken.getIssuedAt());
            assertEquals(authenticationToken.getUserId(), persistedAuthenticationToken.getUserId());
            DateTime expireTime = persistedAuthenticationToken.getExpireAt();
            assertTrue(expireTime.isAfter(beforeRevoke) && expireTime.isBefore(afterRevoke));

        } finally {
            try {
                authenticationTokenTable.deleteExpiredToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testReovkeTokenWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            authenticationTokenTable.revokeToken(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testReovkeTokenWithInvalidRequest");
    }

    @Test
    public void testRevokeTokenWithNonExistedToken() 
            throws ValidationException, RepositoryServerException {
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(SimpleFlakeKeyGenerator.generateKey());
        
        try {
            authenticationTokenTable.revokeToken(authenticationToken.getTokenType(), authenticationToken.getToken());
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testRevokeTokenWithNonExistedToken");
    }
    
    @Test
    public void testRevokeTokenWithExpiredToken() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(SimpleFlakeKeyGenerator.generateKey());
        
        try {
            authenticationTokenTable.persistToken(authenticationToken);
            authenticationTokenTable.revokeToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            Thread.sleep(100);
            try {
                authenticationTokenTable.revokeToken(authenticationToken.getTokenType(), authenticationToken.getToken());
            } catch (ItemNotFoundException error) {
                return;
            } 
            fail("Failed while running testRevokeTokenWithExpiredToken");
        } finally {
          try {
              authenticationTokenTable.deleteExpiredToken(authenticationToken.getTokenType(), authenticationToken.getToken());
          } catch (ItemNotFoundException | RepositoryServerException ignore) {}
      }
    }
    
    // This is same as testPersistTokenHappyCase
    @Test
    public void testGetTokenHappyCase() {}

    @Test
    public void testGetTokenWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            authenticationTokenTable.getToken(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetTokenHappyCase");
    }

    @Test
    public void testGetTokenWithNonExistedToken() 
            throws ValidationException, RepositoryServerException {
        AuthenticationToken authenticationToken = AuthenticationToken.generateAccessTokenForUser(SimpleFlakeKeyGenerator.generateKey());
        
        try {
            authenticationTokenTable.getToken(authenticationToken.getTokenType(), authenticationToken.getToken());
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetTokenWithNonExistedToken");
    }

    @AfterClass
    public static void tearDownAuthenticationTokenTable() throws RepositoryClientException, RepositoryServerException {
//        authenticationTokenTable.deleteTable();
    }
}
