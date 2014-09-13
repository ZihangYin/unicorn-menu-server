package com.unicorn.rest.server.filter;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.internal.util.Base64;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activities.exception.MissingAuthorizationException;
import com.unicorn.rest.activities.exception.UnrecognizedAuthorizationSchemeException;
import com.unicorn.rest.activities.exception.UnrecognizedIdentityException;
import com.unicorn.rest.repository.AuthenticationTokenRepository;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;
import com.unicorn.rest.server.filter.model.AuthenticationScheme;
import com.unicorn.rest.server.filter.model.SubjectPrincipal;
import com.unicorn.rest.server.filter.model.SubjectSecurityContext;
import com.unicorn.rest.server.filter.model.UserSubjectPrincipal;

@Priority(Priorities.AUTHENTICATION)
public class ActivitiesSecurityFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(ActivitiesSecurityFilter.class);
    
    protected static final String AUTHORIZATION_CODE_SEPARATOR = ":";
    
    @Inject
    private AuthenticationTokenRepository tokenRepository;

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String[] authenticationCode = parseAuthenticationHeader(authorizationHeader);
            SubjectPrincipal subjectPrincipal = authenticate(authenticationCode, tokenRepository);
            requestContext.setSecurityContext(new SubjectSecurityContext(subjectPrincipal));
            
        } catch (MissingAuthorizationException | UnrecognizedIdentityException | UnrecognizedAuthorizationSchemeException error) {
            LOG.info(String.format("Failed while attempting to fulfill authorization due to %s: ", BadRequestException.BAD_REQUEST), error);
            throw error;
        } catch (RepositoryServerException error) {
            LOG.error(String.format("Failed while attempting to fulfill authorization due to %s: ", InternalServerErrorException.INTERNAL_FAILURE), error);
            throw new InternalServerErrorException(error);
        }
    }

    private String[] parseAuthenticationHeader(@Nullable String authorizationHeader) 
            throws MissingAuthorizationException, UnrecognizedAuthorizationSchemeException {
        if (StringUtils.isBlank(authorizationHeader)) {
            throw new MissingAuthorizationException();
        }
        
        if (!authorizationHeader.startsWith(AuthenticationScheme.BEARER_AUTHENTICATION.toString())) {
            throw new UnrecognizedAuthorizationSchemeException();
        }
        String authenticationCode = authorizationHeader.replaceFirst(AuthenticationScheme.BEARER_AUTHENTICATION.toString(), "");
        String[] authentication = Base64.decodeAsString(authenticationCode).split(AUTHORIZATION_CODE_SEPARATOR);
        if (authentication.length != 2 || StringUtils.isBlank(authentication[0]) || StringUtils.isBlank(authentication[1])) {
            throw new MissingAuthorizationException();
        }
        return authentication;
    }

    private SubjectPrincipal authenticate(@Nonnull String[] authenticationCode, @Nonnull AuthenticationTokenRepository authenticationTokenRepository) 
            throws MissingAuthorizationException, UnrecognizedIdentityException, RepositoryServerException {
        try {
            Long principal = Long.parseLong(authenticationCode[0]);
            String token = authenticationCode[1];
            
            authenticationTokenRepository.findToken(AuthenticationTokenType.ACCESS_TOKEN, token, principal);
            return new UserSubjectPrincipal(principal, AuthenticationScheme.BEARER_AUTHENTICATION);

        } catch (ValidationException error) {
            throw new MissingAuthorizationException();
        } catch (NumberFormatException | ItemNotFoundException error) {
            throw new UnrecognizedIdentityException();
        }
    }
}
