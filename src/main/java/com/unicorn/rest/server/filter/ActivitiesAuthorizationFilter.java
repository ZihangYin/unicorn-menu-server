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

    @Inject
    private AuthenticationTokenRepository tokenRepository;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String token = parseAuthenticationHeader(authorizationHeader);
        
            SubjectPrincipal subjectPrincipal = authenticate(token, tokenRepository);
            requestContext.setSecurityContext(new SubjectSecurityContext(subjectPrincipal));
            
        } catch (MissingAuthorizationException | UnrecognizedIdentityException error) {
            LOG.info("Failed while attempting to fulfill authorization due to bad request.", error);
            throw error;
        } catch (RepositoryServerException error) {
            LOG.error("Failed while attempting to fulfill authorization due to internal error.", error);
            throw new InternalServerErrorException(error);
        }
    }

    private String parseAuthenticationHeader(String authorizationHeader) 
            throws MissingAuthorizationException, UnrecognizedAuthorizationMethodException {
        if (StringUtils.isBlank(authorizationHeader)) {
            throw new MissingAuthorizationException();
        }

        if (!authorizationHeader.startsWith(AuthenticationScheme.BEARER_AUTHENTICATION.toString())) {
            throw new UnrecognizedAuthorizationMethodException();
        }
        return authorizationHeader.replaceFirst(AuthenticationScheme.BEARER_AUTHENTICATION.toString(), "");
    }

    private SubjectPrincipal authenticate(@Nullable String token, @Nonnull AuthenticationTokenRepository authenticationTokenRepository) 
            throws MissingAuthorizationException, UnrecognizedIdentityException, RepositoryServerException {
        try {
            AuthenticationToken authenticationToken = authenticationTokenRepository.findToken(AuthenticationTokenType.ACCESS_TOKEN, token);
            return new UserSubjectPrincipal(authenticationToken.getUserId(), AuthenticationScheme.BEARER_AUTHENTICATION);

        } catch (ValidationException error) {
            throw new MissingAuthorizationException();
        } catch (ItemNotFoundException error) {
            throw new UnrecognizedIdentityException();
        }
    }
}
