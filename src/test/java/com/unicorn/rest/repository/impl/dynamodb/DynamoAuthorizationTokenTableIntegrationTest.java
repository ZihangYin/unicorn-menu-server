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
import com.unicorn.rest.repository.impl.dynamodb.DynamoAuthorizationTokenTable;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.server.filter.model.PrincipalType;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;
import com.unicorn.rest.utils.TimeUtils;

public class DynamoAuthorizationTokenTableIntegrationTest {
    private static DynamoAuthorizationTokenTable authorizationTokenTable;

    @BeforeClass
    public static void setUpAuthorizationTokenTable() throws RepositoryClientException, RepositoryServerException {
        authorizationTokenTable = new DynamoAuthorizationTokenTable();
        // In case table already exists, exception will be thrown and test will be terminated at this point
//        authorizationTokenTable.createTable();
    }
    
    @Test
    public void testPersistTokenForUserHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            assertEquals(authorizationToken, persistedAuthorizationToken);

        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testPersistTokenForCustomerHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.CUSTOMER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            assertEquals(authorizationToken, persistedAuthorizationToken);

        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testPersistTokenWithInvalidRequest() 
            throws DuplicateKeyException, RepositoryServerException {
        try {
            authorizationTokenTable.persistToken(null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testPersistTokenWithInvalidRequest");
    }
    
    @Test
    public void testPersistTokenWithExistedToken() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        AuthorizationToken anotherAuthorizationToken = AuthorizationToken.buildTokenBuilder(authorizationToken.getToken()).
                tokenType(authorizationToken.getTokenType()).principal(SimpleFlakeKeyGenerator.generateKey())
                .principalType(PrincipalType.CUSTOMER).build();
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            try {
                authorizationTokenTable.persistToken(anotherAuthorizationToken);
            } catch(DuplicateKeyException error) { 
                AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getToken(authorizationToken.getTokenType(), authorizationToken.getToken());
                assertEquals(authorizationToken, persistedAuthorizationToken);
                return;
            }
            fail("Failed while running testPersistTokenWithExistedToken");
        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testRevokeTokenForPrincipalHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            Long beforeRevoke = TimeUtils.getEpochTimeNowInUTC();
            Thread.sleep(100);
            authorizationTokenTable.revokeTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
            Thread.sleep(100);
            Long afterRevoke = TimeUtils.getEpochTimeNowInUTC();
            AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            assertEquals(authorizationToken.getTokenType(), persistedAuthorizationToken.getTokenType());
            assertEquals(authorizationToken.getToken(), persistedAuthorizationToken.getToken());
            assertEquals(authorizationToken.getIssuedAt(), persistedAuthorizationToken.getIssuedAt());
            assertEquals(authorizationToken.getPrincipal(), persistedAuthorizationToken.getPrincipal());
            DateTime expireTime = persistedAuthorizationToken.getExpireAt();
            assertTrue(expireTime.isAfter(beforeRevoke) && expireTime.isBefore(afterRevoke));

        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testRevokeTokenWithExpiredToken() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            authorizationTokenTable.revokeTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
            Thread.sleep(100);
            try {
                authorizationTokenTable.revokeTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
            } catch (ItemNotFoundException error) {
                return;
            } 
            fail("Failed while running testRevokeTokenWithExpiredToken");
        } finally {
          try {
              Thread.sleep(100);
              authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
          } catch (ItemNotFoundException | RepositoryServerException ignore) {}
      }
    }
    
    @Test
    public void testRevokeTokenForPrincipalWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            authorizationTokenTable.revokeTokenForPrincipal(null, null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testReovkeTokenWithInvalidRequest");
    }

    @Test
    public void testRevokeTokenForPrincipalWithNonExistedToken() 
            throws ValidationException, RepositoryServerException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            authorizationTokenTable.revokeTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testRevokeTokenWithNonExistedToken");
    }
    
    @Test
    public void testRevokeTokenForPrincipalWithUnexpectedPrincipal() 
            throws ValidationException, RepositoryServerException {
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(SimpleFlakeKeyGenerator.generateKey(), PrincipalType.USER);
        
        try {
            authorizationTokenTable.revokeTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), SimpleFlakeKeyGenerator.generateKey());
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testRevokeTokenForPrincipalWithUnexpectedPrincipal");
    }
    
    // This is same as testPersistTokenHappyCase
    @Test
    public void testGetTokenHappyCase() {}

    @Test
    public void testGetTokenWithInvalidRequest() 
            throws ItemNotFoundException, RepositoryServerException {
        try {
            authorizationTokenTable.getToken(null, null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetTokenHappyCase");
    }

    @Test
    public void testGetTokenWithNonExistedToken() 
            throws ValidationException, RepositoryServerException {
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(SimpleFlakeKeyGenerator.generateKey(), PrincipalType.USER);
        
        try {
            authorizationTokenTable.getToken(authorizationToken.getTokenType(), authorizationToken.getToken());
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetTokenWithNonExistedToken");
    }

    @Test
    public void testGetTokenForUserHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getTokenForPrincipal
                    (authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
            assertEquals(authorizationToken, persistedAuthorizationToken);

        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }

    @Test
    public void testGetTokenForCustomerHappyCase() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.CUSTOMER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            AuthorizationToken persistedAuthorizationToken = authorizationTokenTable.getTokenForPrincipal
                    (authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
            assertEquals(authorizationToken, persistedAuthorizationToken);

        } finally {
            try {
                Thread.sleep(100);
                authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
            } catch (ItemNotFoundException | RepositoryServerException ignore) {}
        }
    }
    
    @Test
    public void testGetTokenForPrincipalWithInvalidRequest() 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        try {
            authorizationTokenTable.getTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), null);
        } catch (ValidationException error) {
            return;
        } 
        fail("Failed while running testGetTokenForPrincipalWithInvalidRequest");
    }
    
    @Test
    public void testGetTokenForPrincipalWithNonExistedToken() 
            throws ValidationException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            Thread.sleep(100);
            authorizationTokenTable.getTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), principal);
        } catch (ItemNotFoundException error) {
            return;
        } 
        fail("Failed while running testGetTokenForPrincipalWithNonExistedToken");
    }
    
    @Test
    public void testGetTokenForPrincipalWithUnexpectedPrincipal() 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException, InterruptedException {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        PrincipalType principalType = PrincipalType.USER;
        AuthorizationToken authorizationToken = AuthorizationToken.generateAccessToken(principal, principalType);
        
        try {
            authorizationTokenTable.persistToken(authorizationToken);
            authorizationTokenTable.getTokenForPrincipal(authorizationToken.getTokenType(), authorizationToken.getToken(), SimpleFlakeKeyGenerator.generateKey());
        } catch (ItemNotFoundException error) {
            return;
        } finally {
            Thread.sleep(100);
            authorizationTokenTable.deleteExpiredToken(authorizationToken.getTokenType(), authorizationToken.getToken());
        }
        fail("Failed while running testGetTokenForPrincipalWithUnexpectedPrincipal");
    }
    
    @AfterClass
    public static void tearDownAuthorizationTokenTable() throws RepositoryClientException, RepositoryServerException {
//        authorizationTokenTable.deleteTable();
    }
}
