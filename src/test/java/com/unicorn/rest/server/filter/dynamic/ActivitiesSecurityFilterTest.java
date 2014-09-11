package com.unicorn.rest.server.filter.dynamic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.unicorn.rest.activities.exception.MissingAuthorizationException;
import com.unicorn.rest.activities.exception.UnrecognizedAuthorizationSchemeException;
import com.unicorn.rest.activities.exception.UnrecognizedIdentityException;
import com.unicorn.rest.activity.model.ErrorResponse;
import com.unicorn.rest.activity.model.GenerateTokenRequest;
import com.unicorn.rest.activity.model.RevokeTokenRequest;
import com.unicorn.rest.activity.model.TokenResponse;
import com.unicorn.rest.activity.model.UserRequest;
import com.unicorn.rest.activity.model.GenerateTokenRequest.GrantType;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.AuthenticationTokenRepositoryImpl;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.server.GrizzlyServerTestBase;
import com.unicorn.rest.server.filter.model.AuthenticationScheme;
import com.unicorn.rest.server.injector.TestRepositoryBinder;
import com.unicorn.rest.utils.AuthenticationSecretUtils;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;
import com.unicorn.rest.utils.UUIDGenerator;

public class ActivitiesSecurityFilterTest extends GrizzlyServerTestBase {

    private static WebTarget webTarget;
    private static TestRepositoryBinder repositoryBinder;

    @BeforeClass
    public static void setUpWebServer() throws Exception {
        repositoryBinder = new TestRepositoryBinder();
        setUpHttpsWebServer(repositoryBinder);
        webTarget = client.target(uri);
    }

    @After
    public void clearMockedRepository() {
        /*
         * Reset the mocking on this object so that the field can be safely re-used between tests.
         */
        Mockito.reset(repositoryBinder.getMockedTokenRepository());
        Mockito.reset(repositoryBinder.getMockedUserRepository());
    }

    private UserAuthorizationInfo createUserAuthorizationInfo(Long userId, String password) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer hashedPassword = AuthenticationSecretUtils.generateHashedSecretWithSalt(password, salt);
        return UserAuthorizationInfo.buildUserAuthorizationInfo()
                .userId(userId).password(hashedPassword).salt(salt).build();
    }

    private void mockUserAuthenticationHappyCase(String loginName, String password, UserAuthorizationInfo expectedUserAuthorizationInfo) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Long userId = expectedUserAuthorizationInfo.getUserId();
        Mockito.doReturn(userId).when(mockedUserRepository).getUserIdFromLoginName(loginName);
        Mockito.doReturn(expectedUserAuthorizationInfo).when(mockedUserRepository).getUserAuthorizationInfo(userId);
    }

    private void mockTokenPersistencyHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenRevocationHappyCase(AuthenticationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).revokeToken(tokenType, token);
    }

    private void mockCreateNewUserHappyCase(UserRequest userRequest, Long expectedUserId) 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Mockito.doReturn(expectedUserId).when(mockedUserRepository).createUser(
                new UserName(userRequest.getUserName()), new UserDisplayName(userRequest.getUserDisplayName()), userRequest.getPassword());
    }

    private void mockTokenFoundHappyCase(AuthenticationTokenType tokenType, String token, AuthenticationToken authenticationToken) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doReturn(authenticationToken).when(mockedTokenRepository).findToken(tokenType, token);
    }

    private void mockTokenNotFound(AuthenticationTokenType tokenType, String token) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedTokenRepository).findToken(tokenType, token);
    }
    
    /*
     * Happy case
     */
    @Test
    public void testTokenActivitiesGenerateTokenWithoutAuthenticationHeaderHappyCase() throws Exception {
        String loginName = "login_name";
        String password = "1a2b3c";
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(SimpleFlakeKeyGenerator.generateKey(), password));
        mockTokenPersistencyHappyCase();
        Response response = webTarget.path("/v1/tokens").queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        TokenResponse tokenResponse = response.readEntity(TokenResponse.class);

        assertNotNull(tokenResponse);
        assertEquals(AuthenticationTokenType.ACCESS_TOKEN.toString(), tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getExpireAt());
    }

    @Test
    public void testTokenActivitiesRevokeTokenWithoutAuthenticationHeaderHappyCase() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = "token";
        mockTokenRevocationHappyCase(tokenType, token);
        Response response = webTarget.path("/v1/tokens").queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, token).request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }

    @Test
    public void testUserActivitiesCreateNewUserWithoutAuthenticationHeaderHappyCase() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");

        Long userId = SimpleFlakeKeyGenerator.generateKey();
        mockCreateNewUserHappyCase(userRequest, userId);
        Response response = webTarget.path("/v1/users").request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }

    @Test
    public void testHelloWorldActivitiesSayHelloWithAuthenticationHeaderHappyCase() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = UUIDGenerator.randomUUID().toString();
        Long userId = SimpleFlakeKeyGenerator.generateKey();
        AuthenticationToken authenticationToken = AuthenticationToken.buildTokenBuilder(token).tokenType(tokenType).userId(userId).build();

        mockTokenFoundHappyCase(tokenType, token, authenticationToken);
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthenticationScheme.BEARER_AUTHENTICATION + token).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Hello World", response.readEntity(String.class));
    }

    /*
     * Bad Request
     */
    @Test
    public void testHelloWorldActivitiesSayHelloMissingAuthenticationHeader() throws Exception {

        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        MissingAuthorizationException expectedException = new MissingAuthorizationException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloUnrecognizedAuthenticationScheme() throws Exception {

        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthenticationScheme.BASIC_AUTHENTICATION).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        UnrecognizedAuthorizationSchemeException expectedException = new UnrecognizedAuthorizationSchemeException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloUnrecognizedIdentity() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = UUIDGenerator.randomUUID().toString();
        
        mockTokenNotFound(tokenType, token);
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthenticationScheme.BEARER_AUTHENTICATION + token).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        UnrecognizedIdentityException expectedException = new UnrecognizedIdentityException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
}
