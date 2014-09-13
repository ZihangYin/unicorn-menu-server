package com.unicorn.rest.activity.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.apache.commons.collections4.CollectionUtils;

import com.unicorn.rest.activities.exception.BadTokenRequestException;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrCode;
import com.unicorn.rest.activities.exception.TokenErrors.TokenErrDescFormatter;
import com.unicorn.rest.activities.utils.RequestValidator;
import com.unicorn.rest.repository.exception.ValidationException;

@EqualsAndHashCode
public class GenerateTokenRequest {

    public static final String GRANT_TYPE = "grant_type";    
    public static final String LOGIN_NAME = "login_name";
    public static final String PASSWORD = "password";
    public static final String CREDENTIAL = "credential";
    
    public enum GrantType {
        /**
         * user_password is used by end users and customer_credential is used by business customers
         */
        USER_PASSWORD("user_password"),
        CUSTOMER_CREDENTIAL("customer_credential");

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
    private String password;
    private String credential;

    public static GenerateTokenRequest validateGenerateTokenRequest(@Nullable MultivaluedMap<String, String> multiValuedParameters) 
            throws ValidationException {
        if (CollectionUtils.sizeIsEmpty(multiValuedParameters)) {
            throw new ValidationException("Expecting non-null request paramter for validateGenerateTokenRequest, but received: multiValuedParameters=null");
        }

        return new GenerateTokenRequest(multiValuedParameters.getFirst(GRANT_TYPE), 
                multiValuedParameters.getFirst(LOGIN_NAME), multiValuedParameters.getFirst(PASSWORD), 
                multiValuedParameters.getFirst(CREDENTIAL));
    }

    private GenerateTokenRequest(String grantType, String loginName, String password, String credential) 
            throws ValidationException {

        this.loginName = RequestValidator.validateRequiredParameter(LOGIN_NAME, loginName);
        if (GrantType.USER_PASSWORD.toString().equals(grantType)) {
            this.grantType = GrantType.USER_PASSWORD;
            this.password = RequestValidator.validateRequiredParameter(PASSWORD, password);
        } else if (GrantType.CUSTOMER_CREDENTIAL.toString().equals(grantType)) {
            this.credential = RequestValidator.validateRequiredParameter(CREDENTIAL, password);
            this.grantType = GrantType.CUSTOMER_CREDENTIAL;
            
        } else {
            RequestValidator.validateRequiredParameter(GRANT_TYPE, grantType); 
            throw new BadTokenRequestException(TokenErrCode.UNSUPPORTED_GRANT_TYPE,  
                    String.format(TokenErrDescFormatter.UNSUPPORTED_GRANT_TYPE.toString(), grantType));
        }
    }
    
    /**
     * @return password @Nonnull if this is a required parameter for this grant type
     */
    public String getPassword() {
        return password;
    }
    /**
     * @return credential @Nonnull if this is a required parameter for this grant type
     */
    public String getCredential() {
        return credential;
    }

    @Override
    public String toString() {
        return "GenerateTokenRequest [grantType=" + grantType + ", loginName="
                + loginName + "]";
    }
    
}