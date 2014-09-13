package com.unicorn.rest.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.UnsupportedEncodingException;
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

import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.ResourceInUseException;
import com.unicorn.rest.activities.exception.WeakPasswordException;
import com.unicorn.rest.activity.model.ErrorResponse;
import com.unicorn.rest.activity.model.UserRequest;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.impl.UserRepositoryImpl;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.server.GrizzlyServerTestBase;
import com.unicorn.rest.server.injector.TestRepositoryBinder;
import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class UserActivitiesTest extends GrizzlyServerTestBase {
    
    private static WebTarget webTarget;
    private static TestRepositoryBinder repositoryBinder;

    @BeforeClass
    public static void setUpWebServer() throws Exception {
        repositoryBinder = new TestRepositoryBinder();
        setUpHttpsWebServer(repositoryBinder);
        webTarget = client.target(uri).path("v1/users");
    }
    
    @After
    public void clearMockedRepository() {
        /*
         * Reset the mocking on this object so that the field can be safely re-used between tests.
         */
        Mockito.reset(repositoryBinder.getMockedTokenRepository());
        Mockito.reset(repositoryBinder.getMockedUserRepository());
    }
    
    private void mockCreateNewUserHappyCase(UserRequest userRequest, Long expectedUserPrincipal) 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        Mockito.doReturn(expectedUserPrincipal).when(mockedUserRepository).registerUser(
                Name.validateUserName(userRequest.getUserName()), DisplayName.validateUserDisplayName(userRequest.getUserDisplayName()), userRequest.getPassword());
    }
    
    private void mockCreateNewUserDuplicateUserName(UserRequest userRequest) 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        DuplicateKeyException duplicateKey = new DuplicateKeyException();
        Mockito.doThrow(duplicateKey).when(mockedUserRepository).registerUser(
                Name.validateUserName(userRequest.getUserName()), DisplayName.validateUserDisplayName(userRequest.getUserDisplayName()), userRequest.getPassword());
    }
    
    private void mockCreateNewUserServerError(UserRequest userRequest) 
            throws ValidationException, DuplicateKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, RepositoryServerException {
        UserRepositoryImpl mockedUserRepository = repositoryBinder.getMockedUserRepository();
        RepositoryServerException internalError = new RepositoryServerException("Internal Server Error", null);
        Mockito.doThrow(internalError).when(mockedUserRepository).registerUser(
                Name.validateUserName(userRequest.getUserName()), DisplayName.validateUserDisplayName(userRequest.getUserDisplayName()), userRequest.getPassword());
    }
    

    /*
     * Happy Case
     */
    @Test
    public void testCreateNewUserHappyCase() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");
        
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        mockCreateNewUserHappyCase(userRequest, userPrincipal);
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }
    
    @Test
    public void testCreateNewUserWithExtraValueHappyCase() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");
        userRequest.setUserPrincipal(0L);
        userRequest.setEmailAddress(null);
        
        Long userPrincipal = SimpleFlakeKeyGenerator.generateKey();
        mockCreateNewUserHappyCase(userRequest, userPrincipal);
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNull(response.readEntity(Object.class));
    }
    
    /*
     * Bad Request
     */
    @Test
    public void testCreateNewUserMissingUserName() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", UserRequest.USER_NAME)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testCreateNewUserMissingPassword() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setUserDisplayName("user_display_name");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", UserRequest.PASSWORD)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testCreateNewUserMissingUserDisplayName() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        InvalidRequestException expectedException = new InvalidRequestException(
                new ValidationException(String.format("The request is missing required parameters: %s", UserRequest.USER_DISPLAY_NAME)));
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testCreateNewUserInvalidUserName() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name@");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        try {
            Name.validateUserName(userRequest.getUserName());
        } catch(ValidationException error) {
            InvalidRequestException expectedException = new InvalidRequestException(error);
            assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
            assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
            assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
        }
    }
    
    @Test
    public void testCreateNewUserInvalidUserDisplayName() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name_1");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        try {
            DisplayName.validateUserDisplayName(userRequest.getUserDisplayName());
        } catch(ValidationException error) {
            InvalidRequestException expectedException = new InvalidRequestException(error);
            assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
            assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
            assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
        }
    }
    
    @Test
    public void testCreateNewUserWeakPassword() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("weak_password");
        userRequest.setUserDisplayName("user_display_name");
        
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        WeakPasswordException expectedException = new WeakPasswordException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    @Test
    public void testCreateNewUserExistingUserName() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");

        mockCreateNewUserDuplicateUserName(userRequest);
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        ResourceInUseException expectedException = new ResourceInUseException();
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
    
    /*
     * Internal Server Errors
     */
    @Test
    public void testCreateNewUserServerError() throws Exception {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserName("user_name");
        userRequest.setPassword("1a2b3c");
        userRequest.setUserDisplayName("user_display_name");
        
        mockCreateNewUserServerError(userRequest);
        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.entity(userRequest, MediaType.APPLICATION_JSON));
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        ErrorResponse errorResponse= response.readEntity(ErrorResponse.class);
        InternalServerErrorException expectedException = new InternalServerErrorException(null);
        assertEquals(expectedException.getClass().getSimpleName(), errorResponse.getErrorType());
        assertEquals(expectedException.getErrorCode(), errorResponse.getErrorCode());
        assertEquals(expectedException.getErrorDescription(), errorResponse.getErrorDescription());
    }
}
