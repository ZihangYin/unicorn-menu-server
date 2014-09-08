package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;

@EqualsAndHashCode
public class RevokeTokenRequest {

    public static final String TOKEN_TYPE = "token_type";
    public static final String TOKEN = "token";
 
    @Getter @Nonnull private final AuthenticationTokenType tokenType;
    @Getter @Nonnull private final String token;

    public static RevokeTokenRequest validateRevokeTokenRequest(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws ValidationException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new ValidationException("Expecting non-null request paramter for validateRevokeTokenRequest, but received: multiValuedParameters=null");
        }
        return new RevokeTokenRequest(multiValuedParameters.getFirst(TOKEN_TYPE), multiValuedParameters.getFirst(TOKEN));
    }

    private RevokeTokenRequest(@Nullable String tokenType, @Nullable String token) 
            throws ValidationException {
        
        this.tokenType = AuthenticationTokenType.fromString(tokenType);
        this.token = RequestValidator.validateRequiredParameter(TOKEN, token);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("RevokeTokenRequest [tokenType=").append(tokenType);
        builder.append(", token=").append(token);
        builder.append("]");
        return builder.toString();
    }
}
