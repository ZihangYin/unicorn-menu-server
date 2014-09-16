package com.unicorn.rest.server.filter;

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

import org.glassfish.jersey.internal.util.Base64;
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
import com.unicorn.rest.repository.impl.AuthorizationTokenRepositoryImpl;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.model.AuthorizationToken;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.server.GrizzlyServerTestBase;
import com.unicorn.rest.server.filter.ActivitiesSecurityFilter;
import com.unicorn.rest.server.filter.ActivitiesSecurityFilter.AuthorizationScheme;
import com.unicorn.rest.server.filter.model.PrincipalType;
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

    private PrincipalAuthenticationInfo createUserAuthorizationInfo(Long principal, String password) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        ByteBuffer salt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer hashedPassword = AuthenticationSecretUtils.generateHashedSecretWithSalt(password, salt);
        return PrincipalAuthenticationInfo.buildPrincipalAuthenticationInfo()
                .principal(principal).password(hashedPassword).salt(salt).build();
    }

    private void mockUserAuthorizationHappyCase(String loginName, String password, PrincipalAuthenticationInfo expectedUserAuthorizationInfo) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Long userPrincipal = expectedUserAuthorizationInfo.getPrincipal();
        Mockito.doReturn(userPrincipal).when(mockedUserRepository).getPrincipalForLoginName(loginName);
        Mockito.doReturn(expectedUserAuthorizationInfo).when(mockedUserRepository).getAuthenticationInfoForPrincipal(userPrincipal);
    }

    private void mockTokenPersistencyHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthorizationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenRevocationHappyCase(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthorizationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).revokeToken(tokenType, token, principal);
    }

    private void mockCreateNewUserHappyCase(UserRequest userRequest, Long expectedUserPrincipal) 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Mockito.doReturn(expectedUserPrincipal).when(mockedUserRepository).registerUser(
                Name.validateUserName(userRequest.getUserName()), DisplayName.validateUserDisplayName(userRequest.getUserDisplayName()), userRequest.getPassword());
    }

    private void mockTokenFoundHappyCase(AuthorizationTokenType tokenType, String token, AuthorizationToken authorizationToken) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthorizationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doReturn(authorizationToken).when(mockedTokenRepository).findToken(tokenType, token, authorizationToken.getPrincipal());
    }

    private void mockTokenNotFound(AuthorizationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthorizationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doThrow(new ItemNotFoundException()).when(mockedTokenRepository).findToken(tokenType, token, principal);
    }
    
    /*
     * Happy case
     */
    @Test
    public void testGenerateTokenForPasswordHappyCase() throws Exception {
        String loginName = "login_name";
        String password = "1a2b3c";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockUserAuthorizationHappyCase(loginName, password, createUserAuthorizationInfo(principal, password));
        mockTokenPersistencyHappyCase();
        Response response = webTarget.path("v1/tokens").queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        TokenResponse tokenResponse = response.readEntity(TokenResponse.class);

        assertNotNull(tokenResponse);
        assertEquals(AuthorizationTokenType.ACCESS_TOKEN.toString(), tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getExpireAt());
        assertEquals(principal, tokenResponse.getPrincipal());
    }

    @Test
    public void testTokenActivitiesRevokeTokenWithoutAuthorizationHeaderHappyCase() throws Exception {
        AuthorizationTokenType tokenType = AuthorizationTokenType.ACCESS_TOKEN;
        String token = "token";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockTokenRevocationHappyCase(tokenType, token, principal);
        Response response = webTarget.path("/v1/tokens").queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, token).queryParam(RevokeTokenRequest.PRINCIPAL, principal).request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }

    @Test
    public void testUserActivitiesCreateNewUserWithoutAuthorizationHeaderHappyCase() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");

        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        mockCreateNewUserHappyCase(userRequest, userPrincipal);
        Response response = webTarget.path("/v1/users").request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }

    @Test
    public void testHelloWorldActivitiesSayHelloWithAuthorizationHeaderForUserHappyCase() throws Exception {
        AuthorizationTokenType tokenType = AuthorizationTokenType.ACCESS_TOKEN;
        String token = UUIDGenerator.randomUUID().toString();
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        AuthorizationToken authorizationToken = AuthorizationToken.buildTokenBuilder(token).tokenType(tokenType).principal(principal)
                .principalType(PrincipalType.USER).build();

        mockTokenFoundHappyCase(tokenType, token, authorizationToken);
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BEARER_AUTHENTICATION + Base64.encodeAsString(principal + ActivitiesSecurityFilter.AUTHORIZATION_CODE_SEPARATOR + token)).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Hello World", response.readEntity(String.class));
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloWithAuthorizationHeaderForCustomerHappyCase() throws Exception {
        AuthorizationTokenType tokenType = AuthorizationTokenType.ACCESS_TOKEN;
        String token = UUIDGenerator.randomUUID().toString();
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        AuthorizationToken authorizationToken = AuthorizationToken.buildTokenBuilder(token).tokenType(tokenType).principal(principal)
                .principalType(PrincipalType.CUSTOMER).build();

        mockTokenFoundHappyCase(tokenType, token, authorizationToken);
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BEARER_AUTHENTICATION + Base64.encodeAsString(principal + ActivitiesSecurityFilter.AUTHORIZATION_CODE_SEPARATOR + token)).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Hello World", response.readEntity(String.class));
    }

    /*
     * Bad Request
     */
    @Test
    public void testHelloWorldActivitiesSayHelloMissingAuthorizationHeader() throws Exception {

        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        MissingAuthorizationException expectedException = new MissingAuthorizationException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloMissingAuthorizationPrincipal() throws Exception {
        String token = UUIDGenerator.randomUUID().toString();
        
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BEARER_AUTHENTICATION + Base64.encodeAsString(token)).get();
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        MissingAuthorizationException expectedException = new MissingAuthorizationException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloMissingAuthorizationToken() throws Exception {
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BEARER_AUTHENTICATION + Base64.encodeAsString(String.valueOf(principal))).get();
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        MissingAuthorizationException expectedException = new MissingAuthorizationException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloUnrecognizedAuthorizationScheme() throws Exception {

        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BASIC_AUTHENTICATION).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        UnrecognizedAuthorizationSchemeException expectedException = new UnrecognizedAuthorizationSchemeException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testHelloWorldActivitiesSayHelloUnrecognizedIdentity() throws Exception {
        AuthorizationTokenType tokenType = AuthorizationTokenType.ACCESS_TOKEN;
        String token = UUIDGenerator.randomUUID().toString();
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockTokenNotFound(tokenType, token, principal);
        Response response = webTarget.path("/v1/hello").request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, 
                AuthorizationScheme.BEARER_AUTHENTICATION + Base64.encodeAsString(principal + ActivitiesSecurityFilter.AUTHORIZATION_CODE_SEPARATOR + token)).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        UnrecognizedIdentityException expectedException = new UnrecognizedIdentityException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
}
