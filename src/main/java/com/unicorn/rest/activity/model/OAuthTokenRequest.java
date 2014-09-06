package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.exception.InvalidRequestException;
import com.unicorn.rest.activities.exception.OAuthBadRequestException;
import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrCode;
import com.unicorn.rest.activity.model.OAuthErrors.OAuthErrDescFormatter;

@EqualsAndHashCode
public class OAuthTokenRequest {

    public static final String OAUTH_GRANT_TYPE = "grant_type";    
    public static final String OAUTH_LOGIN_NAME = "login_name";
    public static final String OAUTH_PASSWORD = "password";

    public enum GrantType {
        PASSWORD("password");

        private String grantType;

        private GrantType(String grantType) {
            this.grantType = grantType;
        }

        @Override
        public String toString() {
            return grantType;
        }
    }

    @Getter @Nonnull private final GrantType grantType;
    @Getter @Nonnull private final String loginName;
    @Getter @Nonnull private final String password;

    public static OAuthTokenRequest validateRequestFromMultiValuedParameters(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws InvalidRequestException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new InvalidRequestException();
        }

        return new OAuthTokenRequest(multiValuedParameters.getFirst(OAUTH_GRANT_TYPE), 
                multiValuedParameters.getFirst(OAUTH_LOGIN_NAME), multiValuedParameters.getFirst(OAUTH_PASSWORD));
    }

    private OAuthTokenRequest(String grantType, String loginName, 
            String password) throws InvalidRequestException {

        RequestValidator.validateRequiredParameter(grantType); 

        if (GrantType.PASSWORD.toString().equals(grantType)) {
            RequestValidator.validateRequiredParameter(loginName);
            RequestValidator.validateRequiredParameter(password);
            this.grantType = GrantType.PASSWORD;
        } else {
            throw new OAuthBadRequestException(OAuthErrCode.UNSUPPORTED_GRANT_TYPE,  
                    String.format(OAuthErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), grantType));
        }
        this.loginName = loginName;
        this.password = password;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("OAuthTokenRequest [grantType=").append(grantType);
        builder.append(", loginName=").append(loginName);
        builder.append(", password=******]");
        return builder.toString();
    }
}