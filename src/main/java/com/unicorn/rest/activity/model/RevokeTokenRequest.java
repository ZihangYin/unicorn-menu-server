package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthorizationToken.AuthorizationTokenType;

@EqualsAndHashCode
@ToString
public class RevokeTokenRequest {

    public static final String TOKEN_TYPE = "token_type";
    public static final String TOKEN = "token";
    public static final String PRINCIPAL = "principal";
 
    @Getter @Nonnull private final AuthorizationTokenType tokenType;
    @Getter @Nonnull private final String token;
    @Getter @Nonnull private final Long principal;

    public static RevokeTokenRequest validateRevokeTokenRequest(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws ValidationException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new ValidationException("Expecting non-null request paramter for validateRevokeTokenRequest, but received: multiValuedParameters=null");
        }
        return new RevokeTokenRequest(multiValuedParameters.getFirst(TOKEN_TYPE), multiValuedParameters.getFirst(TOKEN), multiValuedParameters.getFirst(PRINCIPAL));
    }

    private RevokeTokenRequest(@Nullable String tokenType, @Nullable String token, @Nullable String principalStr) 
            throws ValidationException {
        
        this.tokenType = AuthorizationTokenType.fromString(tokenType);
        this.token = RequestValidator.validateRequiredParameter(TOKEN, token);
        try {
            this.principal = Long.parseLong(principalStr);
        } catch (NumberFormatException error) {
            throw new ValidationException(String.format("The principal %s is missing or malformed", principalStr));
        }
    }
}
