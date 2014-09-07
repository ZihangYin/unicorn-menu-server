package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.exception.TokenBadRequestException;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrCode;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrDescFormatter;
import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.repository.exception.ValidationException;

@EqualsAndHashCode
public class GenerateTokenRequest {

    public static final String GRANT_TYPE = "grant_type";    
    public static final String LOGIN_NAME = "login_name";
    public static final String PASSWORD = "password";

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

    public static GenerateTokenRequest validateGenerateTokenRequest(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws ValidationException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new ValidationException("Expecting non-null request paramter for validateGenerateTokenRequest, but received: multiValuedParameters=null.");
        }

        return new GenerateTokenRequest(multiValuedParameters.getFirst(GRANT_TYPE), 
                multiValuedParameters.getFirst(LOGIN_NAME), multiValuedParameters.getFirst(PASSWORD));
    }

    private GenerateTokenRequest(String grantType, String loginName, 
            String password) throws ValidationException {

        if (PASSWORD.toString().equals(grantType)) {
            this.grantType = GrantType.PASSWORD;
        } else {
            RequestValidator.validateRequiredParameter(GRANT_TYPE, grantType); 
            throw new TokenBadRequestException(TokenErrCode.UNSUPPORTED_GRANT_TYPE,  
                    String.format(TokenErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), grantType));
        }

        this.loginName = RequestValidator.validateRequiredParameter(LOGIN_NAME, loginName);
        this.password = RequestValidator.validateRequiredParameter(PASSWORD, password);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("GenerateTokenRequest [grantType=").append(grantType);
        builder.append(", loginName=").append(loginName);
        builder.append(", password=******]");
        return builder.toString();
    }
}