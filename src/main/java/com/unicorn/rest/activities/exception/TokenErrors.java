package com.unicorn.rest.activities.exception;

public class TokenErrors {

    public enum TokenErrCode {
        UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
        INVALID_GRANT("invalid_grant"),
        UNRECOGNIZED_TOKEN("unrecognized_token");
        
        private String errCode;

        private TokenErrCode(String errCode) {
            this.errCode = errCode;
        }

        @Override 
        public String toString() {
            return this.errCode;
        }
    }

    public enum TokenErrDescFormatter {
        UNSUPPORTED_GRANT_TYPE ("The authorization grant type %s is not supported by the authorization server."),
        INVALID_GRANT_USER_PASSWORD ("The authentication failed on user %s due to unknown login name or invalid password."),
        INVALID_GRANT_CUSTOMER_CREDENTIAL ("The authentication failed on customer %s due to unknown login name or invalid password."),
        UNRECOGNIZED_TOKEN("The authorization token %s with token type %s does not exist or is already expired.");

        private String errDesc;

        private TokenErrDescFormatter(String errDesc) {
            this.errDesc = errDesc;
        }

        @Override 
        public String toString() {
            return this.errDesc;
        }
    }
}
