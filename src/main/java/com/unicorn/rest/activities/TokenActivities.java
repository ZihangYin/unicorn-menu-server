package com.unicorn.rest.activities;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.BadTokenRequestException;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrCode;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrDescFormatter;
import com.unicorn.rest.activity.model.RevokeTokenRequest;
import com.unicorn.rest.activity.model.GenerateTokenRequest;
import com.unicorn.rest.activity.model.TokenResponse;
import com.unicorn.rest.activity.model.GenerateTokenRequest.GrantType;
import com.unicorn.rest.repository.AuthenticationRepository;
import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.UserRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.PrincipalAuthenticationInfo;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.utils.AuthenticationSecretUtils;

@Path("/v1/tokens")
public class TokenActivities {
    private static final Logger LOG = LogManager.getLogger(TokenActivities.class);

    private static final String GENERATE_TOKEN_ERROR_MESSAGE = "Failed while attempting to fulfill generating token request due to %s: ";
    private static final String REVOKE_TOKEN_ERROR_MESSAGE = "Failed while attempting to fulfill revoking token request due to %s: ";

    private AuthenticationTokenRepository tokenRepository;
    private UserRepository userRepository;

    @Inject
    public TokenActivities(AuthenticationTokenRepository tokenRepository, 
            UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateToken(@Context UriInfo uriInfo) 
            throws BadRequestException, InternalServerErrorException {
        try {
            GenerateTokenRequest tokenRequest = GenerateTokenRequest.validateGenerateTokenRequest(uriInfo.getQueryParameters());
           
            GrantType grantType = tokenRequest.getGrantType();
            AuthenticationToken accessToken = null;
            switch (grantType) {
            case USER_PASSWORD:
                accessToken =  generateToken(tokenRequest.getLoginName(), tokenRequest.getPassword(), 
                        userRepository, TokenErrDescFormatter.INVALID_GRANT_USER_PASSWORD);
                break;
            case CUSTOMER_CREDENTIAL:
            default:
                throw new BadTokenRequestException(TokenErrCode.UNSUPPORTED_GRANT_TYPE,  
                        String.format(TokenErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), grantType));
            }
            TokenResponse tokenResponse = persistAndBuildTokenResponse(accessToken);
            return Response.status(Status.OK).entity(tokenResponse).build();

        } catch (ValidationException badRequest) {
            LOG.info(String.format(GENERATE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), badRequest);
            throw new InvalidRequestException(badRequest);
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(GENERATE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), badRequest);
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
            RevokeTokenRequest revokeTokenRequest = RevokeTokenRequest.validateRevokeTokenRequest(uriInfo.getQueryParameters());
            AuthenticationTokenType tokenType = revokeTokenRequest.getTokenType();
            String token = revokeTokenRequest.getToken();
            Long principal = revokeTokenRequest.getPrincipal();
            
            try {
                tokenRepository.revokeToken(tokenType, token, principal);

            } catch(ItemNotFoundException error) {
                throw new BadTokenRequestException(TokenErrCode.UNRECOGNIZED_TOKEN, 
                        String.format(TokenErrDescFormatter.UNRECOGNIZED_TOKEN.toString(), token, tokenType));
            }

            return Response.status(Status.OK).build();

        } catch (ValidationException badRequest) {
            LOG.info(String.format(REVOKE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), badRequest);
            throw new InvalidRequestException(badRequest);
        } catch (BadRequestException badRequest) {
            LOG.info(String.format(REVOKE_TOKEN_ERROR_MESSAGE, BadRequestException.BAD_REQUEST), badRequest);
            throw badRequest;
        } catch (Exception internalFailure) {
            LOG.error(String.format(REVOKE_TOKEN_ERROR_MESSAGE, InternalServerErrorException.INTERNAL_FAILURE), internalFailure);
            throw new InternalServerErrorException(internalFailure);
        }
    }

    private @Nonnull AuthenticationToken generateToken(@Nonnull String loginName, String clientSecret, 
            AuthenticationRepository authenticationRepository, TokenErrDescFormatter tokenErrDescFormatter) 
            throws BadTokenRequestException, ValidationException, RepositoryServerException, UnsupportedEncodingException, NoSuchAlgorithmException {
        try {
            Long principal = authenticationRepository.getPrincipalFromLoginName(loginName);
            PrincipalAuthenticationInfo authenticationInfo = authenticationRepository.getAuthorizationInfo(principal);

            if (AuthenticationSecretUtils.authenticateSecret(clientSecret, authenticationInfo.getPassword(), authenticationInfo.getSalt())) {
                return AuthenticationToken.generateTokenBuilder().tokenType(AuthenticationTokenType.ACCESS_TOKEN).principal(principal).build();
            }

            throw new BadTokenRequestException(TokenErrCode.INVALID_GRANT,  
                    String.format(tokenErrDescFormatter.toString(), loginName));

        }  catch (ItemNotFoundException error) {
            throw new BadTokenRequestException(TokenErrCode.INVALID_GRANT,  
                    String.format(tokenErrDescFormatter.toString(), loginName));
        }
    }
    

    private @Nonnull TokenResponse persistAndBuildTokenResponse(@Nullable AuthenticationToken accessToken) throws ValidationException, RepositoryServerException {
        try {
            tokenRepository.persistToken(accessToken);

        } catch (DuplicateKeyException duplicateKeyOnce) {
            /**
             * Here we try one more time to persist the token only if we get back DuplicateKeyException. 
             * If we still fail after that, throw exception and log an error.
             * 
             * TODO: monitor how often this happens
             */
            LOG.warn("Failed to persist token {} due to duplicate token already exists.", accessToken.getToken());
            accessToken = AuthenticationToken.updateTokenValue(accessToken);
            try {
                tokenRepository.persistToken(accessToken);

            } catch (DuplicateKeyException duplicateKeyAgain) {
                LOG.error("Failed to persist token {} for the second time due to duplicate token already exists.", accessToken.getToken());
                throw new RepositoryServerException(duplicateKeyAgain);
            }
        }
        return new TokenResponse(accessToken);
    }
}
