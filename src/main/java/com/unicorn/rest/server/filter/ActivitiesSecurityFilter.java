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
import com.unicorn.rest.repository.AuthorizationTokenRepository;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;
import com.unicorn.rest.server.filter.model.CustomerPrincipal;
import com.unicorn.rest.server.filter.model.PrincipalType;
import com.unicorn.rest.server.filter.model.SubjectPrincipal;
import com.unicorn.rest.server.filter.model.PrincipalSecurityContext;
import com.unicorn.rest.server.filter.model.UserPrincipal;

@Priority(Priorities.AUTHENTICATION)
public class ActivitiesSecurityFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(ActivitiesSecurityFilter.class);
    
    protected static final String AUTHORIZATION_CODE_SEPARATOR = ":";
    
    @Inject
    private AuthorizationTokenRepository tokenRepository;
    
    public enum AuthorizationScheme {
        
        BASIC_AUTHENTICATION("Basic "),
        BEARER_AUTHENTICATION("Bearer ");

        private String authSchemePrefix;

        private AuthorizationScheme(String authSchemePrefix) {
            this.authSchemePrefix = authSchemePrefix;
        }

        @Override
        public String toString() {
            return this.authSchemePrefix;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String[] authorizationCode = parseAuthorizationHeader(authorizationHeader);
            SubjectPrincipal subjectPrincipal = authenticate(authorizationCode);
            requestContext.setSecurityContext(new PrincipalSecurityContext(subjectPrincipal));
            
        } catch (MissingAuthorizationException | UnrecognizedIdentityException | UnrecognizedAuthorizationSchemeException error) {
            LOG.info(String.format("Failed while attempting to fulfill authorization due to %s: ", BadRequestException.BAD_REQUEST), error);
            throw error;
        } catch (RepositoryServerException error) {
            LOG.error(String.format("Failed while attempting to fulfill authorization due to %s: ", InternalServerErrorException.INTERNAL_FAILURE), error);
            throw new InternalServerErrorException(error);
        }
    }

    private String[] parseAuthorizationHeader(@Nullable String authorizationHeader) 
            throws MissingAuthorizationException, UnrecognizedAuthorizationSchemeException {
        if (StringUtils.isBlank(authorizationHeader)) {
            throw new MissingAuthorizationException();
        }
        
        if (!authorizationHeader.startsWith(AuthorizationScheme.BEARER_AUTHENTICATION.toString())) {
            throw new UnrecognizedAuthorizationSchemeException();
        }
        String authorizationCode = authorizationHeader.replaceFirst(AuthorizationScheme.BEARER_AUTHENTICATION.toString(), "");
        String[] authorization = Base64.decodeAsString(authorizationCode).split(AUTHORIZATION_CODE_SEPARATOR);
       
        if (authorization.length != 2 || StringUtils.isBlank(authorization[0]) || StringUtils.isBlank(authorization[1])) {
            throw new MissingAuthorizationException();
        }
        
        return authorization;
    }

    private SubjectPrincipal authenticate(@Nonnull String[] authorizationCode) 
            throws MissingAuthorizationException, UnrecognizedIdentityException, RepositoryServerException {
        try {
            Long principal = Long.parseLong(authorizationCode[0]);
            String token = authorizationCode[1];
            
            PrincipalType principalType = tokenRepository.findToken(AuthorizationTokenType.ACCESS_TOKEN, token, principal).getPrincipalType();
            
            if (PrincipalType.CUSTOMER.equals(principalType)) {
                return new CustomerPrincipal(principal, AuthorizationScheme.BEARER_AUTHENTICATION);
            } else if (PrincipalType.USER.equals(principalType)) { 
                return new UserPrincipal(principal, AuthorizationScheme.BEARER_AUTHENTICATION);
            } 
            throw new UnrecognizedIdentityException();
            
        } catch (ValidationException error) {
            throw new MissingAuthorizationException();
        } catch (NumberFormatException | ItemNotFoundException error) {
            throw new UnrecognizedIdentityException();
        }
    }
}
