package com.unicorn.rest.activities;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.OAuthBadRequestException;
import com.unicorn.rest.activity.model.OAuthRevokeTokenRequest;
import com.unicorn.rest.activity.model.OAuthTokenRequest;
import com.unicorn.rest.activity.model.OAuthTokenResponse;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrCode;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrDescFormatter;
import com.unicorn.rest.activity.model.OAuthTokenRequest.GrantType;
import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.UserAuthorizationInfo;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.utils.PasswordAuthenticationHelper;

@Path("/oauth2/v1/token")
public class OAuthTokenActivities {
    private static final Logger LOG = LogManager.getLogger(OAuthTokenActivities.class);

    private static final String GENERATE_TOKEN_ERROR_MESSAGE = "Failed while attempting to fulfill generating token request due to %s: ";
    private static final String REVOKE_TOKEN_ERROR_MESSAGE = "Failed while attempting to fulfill revoking token request due to %s: ";

    private AuthenticationTokenRepository tokenRepository;
    private UserRepository userRepository;

    @Inject
    public OAuthTokenActivities(AuthenticationTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateToken(@Context UriInfo uriInfo) 
            throws BadRequestException, InternalServerErrorException {
        try {
            OAuthTokenRequest oAuthTokenRequest = OAuthTokenRequest.validateRequestFromMultiValuedParameters(uriInfo.getQueryParameters());
            return generateToken(oAuthTokenRequest);
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(GENERATE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST) + badRequest.getMessage());
            throw badRequest;

        } catch (Exception internalFailure) {
            LOG.error(String.format(GENERATE_TOKEN_ERROR_MESSAGE, InternalServerErrorException.INTERNAL_FAILURE), internalFailure);
            throw new InternalServerErrorException(internalFailure);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateToken(final MultivaluedMap<String, String> formParameters) 
            throws BadRequestException, InternalServerErrorException {
        try {
            OAuthTokenRequest oAuthTokenRequest = OAuthTokenRequest.validateRequestFromMultiValuedParameters(formParameters);
            return generateToken(oAuthTokenRequest);
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(GENERATE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST) + badRequest.getMessage());
            throw badRequest;

        } catch (Exception internalFailure) {
            LOG.error(String.format(GENERATE_TOKEN_ERROR_MESSAGE, InternalServerErrorException.INTERNAL_FAILURE), internalFailure);
            throw new InternalServerErrorException(internalFailure);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeToken(@Context UriInfo uriInfo) 
            throws BadRequestException, InternalServerErrorException {
        try {
            OAuthRevokeTokenRequest oAuthRevokeTokenRequest = OAuthRevokeTokenRequest.validateRequestFromMultiValuedParameters(uriInfo.getQueryParameters());
            AuthenticationTokenType tokenType = oAuthRevokeTokenRequest.getTokenType();
            String token = oAuthRevokeTokenRequest.getToken();
            try {
                tokenRepository.revokeToken(tokenType, token);
            } catch (ValidationException | ItemNotFoundException error) {
                throw new OAuthBadRequestException(OAuthErrCode.UNRECOGNIZED_TOKEN, 
                        String.format(OAuthErrDescFormatter.UNRECOGNIZED_TOKEN.toString(), token, tokenType));
            }
            
            return Response.status(Status.OK).build();
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(REVOKE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST) + badRequest.getMessage());
            throw badRequest;

        } catch (Exception internalFailure) {
            LOG.error(String.format(REVOKE_TOKEN_ERROR_MESSAGE, InternalServerErrorException.INTERNAL_FAILURE), internalFailure);
            throw new InternalServerErrorException(internalFailure);
        }
    }

    private Response generateToken(@Nonnull OAuthTokenRequest oAuthTokenRequest) 
            throws OAuthBadRequestException, ValidationException, DuplicateKeyException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        AuthenticationToken accessToken = null;
        GrantType oauthGrantType = oAuthTokenRequest.getGrantType();        

        switch (oauthGrantType) {
        case PASSWORD:
            accessToken = generateTokenForPasswordGrant(oAuthTokenRequest);
            break;
        default:
            throw new OAuthBadRequestException(OAuthErrCode.UNSUPPORTED_GRANT_TYPE,  
                    String.format(OAuthErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), oauthGrantType));
        }

        // Here we try one more time to persist the token only if we get back DuplicateKeyException. 
        // If we still fail after that, throw exception and log a fatal.
        try {
            tokenRepository.persistToken(accessToken);
        } catch (DuplicateKeyException duplicateKeyOnce) {
            //TODO: monitor how often this happens
            LOG.warn( String.format("Failed to persist token %s due to duplicate token already exists", accessToken.getToken()));
            accessToken = AuthenticationToken.updateTokenValue(accessToken);
            try {
                tokenRepository.persistToken(accessToken);
            } catch (DuplicateKeyException duplicateKeyAgain) {
                LOG.error(String.format("Failed to persist token %s for the second time due to duplicate token already exists", accessToken.getToken()));
                throw duplicateKeyAgain;
            }
        }
        OAuthTokenResponse oauthTokenResponse = new OAuthTokenResponse(accessToken);
        
        return Response.status(Status.OK).entity(oauthTokenResponse).build();
    }

    private @Nonnull AuthenticationToken generateTokenForPasswordGrant(@Nonnull OAuthTokenRequest oAuthTokenRequest) 
            throws OAuthBadRequestException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String loginName = oAuthTokenRequest.getLoginName();
        String password = oAuthTokenRequest.getPassword();
        
        try {
            Long userId = userRepository.getUserIdFromLoginName(loginName);
            UserAuthorizationInfo userAuthorizationInfo = userRepository.getUserAuthorizationInfo(userId);

            if (PasswordAuthenticationHelper.authenticatePassword(password, userAuthorizationInfo.getPassword(), userAuthorizationInfo.getSalt())) {
                return AuthenticationToken.generateTokenBuilder().tokenType(AuthenticationTokenType.ACCESS_TOKEN).userId(userId).build();
            }
            throw new OAuthBadRequestException(OAuthErrCode.INVALID_GRANT,  
                    String.format(OAuthErrDescFormatter.INVALID_GRANT_PASSWORD.toString(), loginName));
            
        } catch (ValidationException | ItemNotFoundException error) {
            throw new OAuthBadRequestException(OAuthErrCode.INVALID_GRANT,  
                    String.format(OAuthErrDescFormatter.INVALID_GRANT_PASSWORD.toString(), loginName));
        }
    }
}
