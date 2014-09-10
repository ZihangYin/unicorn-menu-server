package com.unicorn.rest.server.filter;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.internal.util.Base64;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.MissingAuthorizationException;
import com.unicorn.rest.activities.exception.UnrecognizedAuthorizationMethodException;
import com.unicorn.rest.activities.exception.UnrecognizedIdentityException;
import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.server.filter.model.AuthenticationScheme;
import com.unicorn.rest.server.filter.model.SubjectPrincipal;
import com.unicorn.rest.server.filter.model.SubjectSecurityContext;
import com.unicorn.rest.server.filter.model.UserSubjectPrincipal;

//@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ActivitiesAuthorizationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(ActivitiesAuthorizationFilter.class);
    private static final String AUTHORIZATION_CODE_SEPARATOR = ":";
    
    @Inject
    private AuthenticationTokenRepository tokenRepository;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String[] authentication = parseAuthenticationHeader(authorizationHeader);
        
            SubjectPrincipal subjectPrincipal = authenticate(authentication, tokenRepository);
            requestContext.setSecurityContext(new SubjectSecurityContext(subjectPrincipal));
            
        } catch (MissingAuthorizationException | UnrecognizedIdentityException | UnrecognizedAuthorizationMethodException error) {
            LOG.info(String.format("Failed while attempting to fulfill authorization due to %s: ", BadRequestException.BAD_REQUEST), error);
            throw error;
        } catch (RepositoryServerException error) {
            LOG.error(String.format("Failed while attempting to fulfill authorization due to %s: ", InternalServerErrorException.INTERNAL_FAILURE), error);
            throw new InternalServerErrorException(error);
        }
    }

    private String[] parseAuthenticationHeader(@Nullable String authorizationHeader) 
            throws MissingAuthorizationException, UnrecognizedAuthorizationMethodException {
        if (StringUtils.isBlank(authorizationHeader)) {
            throw new MissingAuthorizationException();
        }

        if (!authorizationHeader.startsWith(AuthenticationScheme.BEARER_AUTHENTICATION.toString())) {
            throw new UnrecognizedAuthorizationMethodException();
        }
        String authorizationCode = authorizationHeader.replaceFirst(AuthenticationScheme.BEARER_AUTHENTICATION.toString(), "");
        //Decode the Base64 into byte[]
        String[] authentication = Base64.decodeAsString(authorizationCode).split(AUTHORIZATION_CODE_SEPARATOR);
        if (authentication.length != 2 || StringUtils.isBlank(authentication[0]) || StringUtils.isBlank(authentication[1])) {
            throw new MissingAuthorizationException();
        } 
        return authentication;
    }

    private SubjectPrincipal authenticate(@Nonnull String[] authentication, @Nonnull AuthenticationTokenRepository authenticationTokenRepository) 
            throws MissingAuthorizationException, UnrecognizedIdentityException, RepositoryServerException {
        try {
            AuthenticationToken authenticationToken = authenticationTokenRepository.findToken(AuthenticationTokenType.ACCESS_TOKEN, authentication[1]);
            if (!authentication[0].equals(authenticationToken.getUserId())) {
                throw new UnrecognizedIdentityException();
            }
            return new UserSubjectPrincipal(authenticationToken.getUserId(), AuthenticationScheme.BEARER_AUTHENTICATION);

        } catch (ValidationException error) {
            throw new MissingAuthorizationException();
        } catch (ItemNotFoundException error) {
            throw new UnrecognizedIdentityException();
        }
    }
}
