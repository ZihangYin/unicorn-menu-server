package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.OAuthBadRequestException;
import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrCode;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrDescFormatter;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.AuthenticationToken.AuthenticationTokenType;

@EqualsAndHashCode
@ToString
public class OAuthRevokeTokenRequest {

    public static final String OAUTH_TOKEN_TYPE = "token_type";
    public static final String OAUTH_TOKEN = "token";
 
    @Getter @Nonnull private final AuthenticationTokenType tokenType;
    @Getter @Nonnull private final String token;

    public static OAuthRevokeTokenRequest validateRequestFromMultiValuedParameters(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws InvalidRequestException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new InvalidRequestException();
        }
        return new OAuthRevokeTokenRequest(multiValuedParameters.getFirst(OAUTH_TOKEN_TYPE), multiValuedParameters.getFirst(OAUTH_TOKEN));
    }

    private OAuthRevokeTokenRequest(@Nullable String tokenType, @Nullable String token) throws InvalidRequestException {
        
        String tokenTypeStr = RequestValidator.validateRequiredParameter(tokenType); 
        try {
             this.tokenType = AuthenticationTokenType.fromString(tokenTypeStr);
        } catch (ValidationException error) {
            throw new OAuthBadRequestException(OAuthErrCode.UNSUPPORTED_TOKEN_TYPE,  
                    String.format(OAuthErrDescFormatter.UNSUPPORTED_TOKEN_TYPE.toString(), tokenTypeStr));
        }
        
        this.token = RequestValidator.validateRequiredParameter(token);
    }
}
