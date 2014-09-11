package com.unicorn.rest.activities;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.ResourceInUseException;
import com.unicorn.rest.activities.exception.WeakPasswordException;
import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.activity.model.UserRequest;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.UserDisplayName;
import com.unicorn.rest.repository.model.UserName;
import com.unicorn.rest.utils.AuthenticationSecretUtils;

@Path("/v1/users")
public class UserActivities {
    private static final Logger LOG = LogManager.getLogger(UserActivities.class);

    private static final String CREATE_USER_ERROR_MESSAGE = "Failed while attempting to fulfill creating new user request due to %s: ";

    private UserRepository userRepository;

    @Inject
    public UserActivities(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewUser(UserRequest userRequest) 
            throws BadRequestException, InternalServerErrorException {
        try {
            if (userRequest == null) {
                throw new ValidationException("Expecting non-null request paramter for createNewUser, but received: userRequest=null");
            }
            
            UserName userName = new UserName(RequestValidator.validateRequiredParameter(UserRequest.USER_NAME, userRequest.getUserName()));
            UserDisplayName userDisplayName = new UserDisplayName(RequestValidator.validateRequiredParameter(UserRequest.USER_DISPLAY_NAME, userRequest.getUserDisplayName()));
            String password = RequestValidator.validateRequiredParameter(UserRequest.PASSWORD, userRequest.getPassword());
            if (!AuthenticationSecretUtils.validateStrongSecret(password)) {
                throw new WeakPasswordException();
            }
            try{    
                userRepository.createUser(userName, userDisplayName, password);
                return Response.status(Status.OK).build();
            } catch (DuplicateKeyException error) {
                throw new ResourceInUseException();
            }

        } catch (ValidationException error) {
            LOG.info(String.format(CREATE_USER_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), error);
            throw new InvalidRequestException(error);
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(CREATE_USER_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), badRequest);
            throw badRequest;
        } catch (Exception internalFailure) {
            LOG.error(String.format(CREATE_USER_ERROR_MESSAGE, InternalServerErrorException.INTERNAL_FAILURE), internalFailure);
            throw new InternalServerErrorException(internalFailure);
        }
    }

}