package com.unicorn.rest.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.unicorn.rest.activities.exception.BadTokenRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrCode;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrDescFormatter;
import com.unicorn.rest.activity.model.ErrorResponse;
import com.unicorn.rest.activity.model.RevokeTokenRequest;
import com.unicorn.rest.activity.model.GenerateTokenRequest;
import com.unicorn.rest.activity.model.TokenResponse;
import com.unicorn.rest.activity.model.GenerateTokenRequest.GrantType;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.AuthenticationTokenRepositoryImpl;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.server.GrizzlyServerTestBase;
import com.unicorn.rest.server.injector.TestRepositoryBinder;
import com.unicorn.rest.utils.AuthenticationSecretUtils;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class TokenActivitiesTest extends GrizzlyServerTestBase {

    private static WebTarget webTarget;
    private static TestRepositoryBinder repositoryBinder;

    @BeforeClass
    public static void setUpWebServer() throws Exception {
        repositoryBinder = new TestRepositoryBinder();
        setUpHttpsWebServer(repositoryBinder);
        webTarget = client.target(uri).path("v1/tokens");
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
        return PrincipalAuthenticationInfo.buildPrincipalAuthorizationInfo()
                .principal(principal).password(hashedPassword).salt(salt).build();
    }
    
    private void mockUserAuthenticationHappyCase(String loginName, String password, PrincipalAuthenticationInfo expectedUserAuthorizationInfo) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Long principal = expectedUserAuthorizationInfo.getPrincipal();
        Mockito.doReturn(principal).when(mockedUserRepository).getPrincipalForLoginName(loginName);
        Mockito.doReturn(expectedUserAuthorizationInfo).when(mockedUserRepository).getAuthorizationInfoForPrincipal(principal);
    }

    private void mockUserAuthenticationNoUser(String loginName, String password) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        ItemNotFoundException itemNotFound = new ItemNotFoundException();
        Mockito.doThrow(itemNotFound).when(mockedUserRepository).getPrincipalForLoginName(loginName);
    }

    private void mockTokenPersistencyHappyCase() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenPersistencyDuplicateTokenOnce() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        DuplicateKeyException duplicateKey = new DuplicateKeyException();
        Mockito.doThrow(duplicateKey).doNothing().when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenPersistencyDuplicateToken() 
            throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        DuplicateKeyException duplicateKey = new DuplicateKeyException();
        Mockito.doThrow(duplicateKey).when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenPersistencyServerError() throws ValidationException, DuplicateKeyException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        RepositoryServerException internalError = new RepositoryServerException("Internal Server Error", null);
        Mockito.doThrow(internalError).when(mockedTokenRepository).persistToken(Mockito.any());
    }

    private void mockTokenRevocationHappyCase(AuthenticationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        Mockito.doNothing().when(mockedTokenRepository).revokeToken(tokenType, token, principal);
    }

    private void mockTokenRevocationNoToken(AuthenticationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        ItemNotFoundException itemNotFound = new ItemNotFoundException();
        Mockito.doThrow(itemNotFound).when(mockedTokenRepository).revokeToken(tokenType, token, principal);
    }

    private void mockTokenRevocationServerError(AuthenticationTokenType tokenType, String token, Long principal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        AuthenticationTokenRepositoryImpl mockedTokenRepository = repositoryBinder.getMockedTokenRepository();
        RepositoryServerException internalError = new RepositoryServerException("Internal Server Error", null);
        Mockito.doThrow(internalError).when(mockedTokenRepository).revokeToken(tokenType, token, principal);
    }

    /*
     * Happy Case
     */
    @Test
    public void testGenerateTokenForPasswordHappyCase() throws Exception {
        String loginName = "login_name";
        String password = "1a2b3c";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(principal, password));
        mockTokenPersistencyHappyCase();
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        TokenResponse tokenResponse = response.readEntity(TokenResponse.class);

        assertNotNull(tokenResponse);
        assertEquals(AuthenticationTokenType.ACCESS_TOKEN.toString(), tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getExpireAt());
        assertEquals(principal, tokenResponse.getPrincipal());
    }

    @Test
    public void testGenerateTokenForPasswordWithDuplicateTokenOnceHappyCase() throws Exception {

        String loginName = "login_name";
        String password = "1a2b3c";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(principal, password));
        mockTokenPersistencyDuplicateTokenOnce();

        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        TokenResponse tokenResponse = response.readEntity(TokenResponse.class);
        assertNotNull(tokenResponse);
        assertEquals(AuthenticationTokenType.ACCESS_TOKEN.toString(), tokenResponse.getTokenType());
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getExpireAt());
        assertEquals(principal, tokenResponse.getPrincipal());
    }

    @Test
    public void testTokenActivitiesRevokeTokenWithoutAuthenticationHeaderHappyCase() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = "token";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockTokenRevocationHappyCase(tokenType, token, principal);
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, token).queryParam(RevokeTokenRequest.PRINCIPAL, principal).request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }

    /*
     * Bad Request
     */
    @Test
    public void testGenerateTokenMissingGrantType() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, "login_name")
                .queryParam(GenerateTokenRequest.PASSWORD, "1a2b3c").request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.GRANT_TYPE)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }

    @Test
    public void testGenerateTokenEmptyGrantType() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, "login_name")
                .queryParam(GenerateTokenRequest.PASSWORD, "1a2b3c")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, new String()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.GRANT_TYPE)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }

    @Test
    public void testGenerateTokenUnsupportedGrantType() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, "login_name")
                .queryParam(GenerateTokenRequest.PASSWORD, "1a2b3c")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, "unsupported_grant_type")
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        assertEquals(BadTokenRequestException.class.getSimpleName(), errorResponse.getErrorType());
        assertEquals(TokenErrCode.UNSUPPORTED_GRANT_TYPE.toString(), errorResponse.getErrorCode());
        assertEquals( String.format(TokenErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), "unsupported_grant_type"), 
                errorResponse.getErrorDescription());
    }

    @Test
    public void testGenerateTokenForUserPasswordMissingLoginName() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.PASSWORD, "1a2b3c")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.LOGIN_NAME)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    

    @Test
    public void testGenerateTokenForPasswordMissingPassword() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, "login_name")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.PASSWORD)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testGenerateTokenForCustomerCredentialMissingLoginName() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.CREDENTIAL, "1a2b3c")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.CUSTOMER_CREDENTIAL.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.LOGIN_NAME)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testGenerateTokenForCustomerCredentialMissingCredential() throws Exception {
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, "login_name")
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.CUSTOMER_CREDENTIAL.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", GenerateTokenRequest.CREDENTIAL)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }


    @Test
    public void testGenerateTokenForPasswordUserDoesNotExist() throws Exception {
        String loginName = "login_name";
        String password = "1a2b3c";
        mockUserAuthenticationNoUser(loginName, password);
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        assertEquals(BadTokenRequestException.class.getSimpleName(), errorResponse.getErrorType());
        assertEquals(TokenErrCode.INVALID_GRANT.toString(), errorResponse.getErrorCode());
        assertEquals(String.format(TokenErrDescFormatter.INVALID_GRANT_USER_PASSWORD.toString(), loginName), 
                errorResponse.getErrorDescription());
    }
    
    @Test
    public void testGenerateTokenForPasswordWrongPassword() throws Exception {
        String loginName = "login_name";
        String password = "1a2b3c";
        
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(SimpleFlakeKeyGenerator.generateKey(), password));
        String wrongPassword = "wrong_password";
        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, wrongPassword)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        assertEquals(BadTokenRequestException.class.getSimpleName(), errorResponse.getErrorType());
        assertEquals(TokenErrCode.INVALID_GRANT.toString(), errorResponse.getErrorCode());
        assertEquals(String.format(TokenErrDescFormatter.INVALID_GRANT_USER_PASSWORD.toString(), loginName), 
                errorResponse.getErrorDescription());
    }

    @Test
    public void testRevokeTokenFailedToRevokeTokenUnrecognizedTokenType() throws Exception {
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, "wrong_token_type")
                .queryParam(RevokeTokenRequest.TOKEN, "token").queryParam(RevokeTokenRequest.PRINCIPAL, SimpleFlakeKeyGenerator.generateKey())
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The authentication token type %s is not supported by the authorization server", "wrong_token_type")));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testRevokeTokenFailedToRevokeTokenMissingTokenType() throws Exception {
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN, "token")
                .queryParam(RevokeTokenRequest.PRINCIPAL, SimpleFlakeKeyGenerator.generateKey())
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException("The authentication token type null is not supported by the authorization server"));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testRevokeTokenFailedToRevokeTokenMissingToken() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.PRINCIPAL, SimpleFlakeKeyGenerator.generateKey())
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", RevokeTokenRequest.TOKEN)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testRevokeTokenFailedToRevokeTokenMissingPrincipal() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, "token")
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException("The principal null is missing or malformed"));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testRevokeTokenFailedToRevokeTokenInvalidFormatPrincipal() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, "token")
                .queryParam(RevokeTokenRequest.PRINCIPAL, "invalid_principal")
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException( String.format("The principal %s is missing or malformed", "invalid_principal")));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }

    @Test
    public void testRevokeTokenFailedToRevokeTokenTokenDoesNotExist() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = "token";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockTokenRevocationNoToken(tokenType, token, principal);
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, token).queryParam(RevokeTokenRequest.PRINCIPAL, principal)
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        assertEquals(BadTokenRequestException.class.getSimpleName(), errorResponse.getErrorType());
        assertEquals(TokenErrCode.UNRECOGNIZED_TOKEN.toString(), errorResponse.getErrorCode());
        assertEquals(String.format(TokenErrDescFormatter.UNRECOGNIZED_TOKEN.toString(), "token", AuthenticationTokenType.ACCESS_TOKEN), 
                errorResponse.getErrorDescription());
    }

    /*
     * Internal Server Errors
     */
    @Test
    public void testGenerateTokenFailedToPersisTokenDuplicateToken() throws Exception {

        String loginName = "login_name";
        String password = "1a2b3c";
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(SimpleFlakeKeyGenerator.generateKey(), password));
        mockTokenPersistencyDuplicateToken();

        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InternalServerErrorException expectedException = new InternalServerErrorException(null);
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }

    @Test
    public void testGenerateTokenFailedToPersisTokenServerError() throws Exception {

        String loginName = "login_name";
        String password = "1a2b3c";
        mockUserAuthenticationHappyCase(loginName, password, createUserAuthorizationInfo(SimpleFlakeKeyGenerator.generateKey(), password));
        mockTokenPersistencyServerError();

        Response response = webTarget.queryParam(GenerateTokenRequest.LOGIN_NAME, loginName)
                .queryParam(GenerateTokenRequest.PASSWORD, password)
                .queryParam(GenerateTokenRequest.GRANT_TYPE, GrantType.USER_PASSWORD.toString()).request(MediaType.APPLICATION_JSON).get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InternalServerErrorException expectedException = new InternalServerErrorException(null);
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }

    @Test
    public void testRevokeTokenFailedToRevokeTokenServerError() throws Exception {
        AuthenticationTokenType tokenType = AuthenticationTokenType.ACCESS_TOKEN;
        String token = "token";
        Long principal = SimpleFlakeKeyGenerator.generateKey();
        
        mockTokenRevocationServerError(tokenType, token, principal);
        Response response = webTarget.queryParam(RevokeTokenRequest.TOKEN_TYPE, tokenType)
                .queryParam(RevokeTokenRequest.TOKEN, token)
                .queryParam(RevokeTokenRequest.PRINCIPAL, principal)
                .request(MediaType.APPLICATION_JSON).delete();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        assertNotNull(errorResponse);
        InternalServerErrorException expectedException = new InternalServerErrorException(null);
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
}
