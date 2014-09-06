package com.unicorn.rest.activity.model;

public class OAuthErrors {

    public enum OAuthErrCode {
        UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
        INVALID_GRANT("invalid_grant"),
        UNSUPPORTED_TOKEN_TYPE("unsupported_token_type"),
        UNRECOGNIZED_TOKEN("unrecognized_token");
        
        private String errCode;

        private OAuthErrCode(String errCode) {
            this.errCode = errCode;
        }

        @Override 
        public String toString() {
            return this.errCode;
        }
    }

    public enum OAuthErrDescFormatter {
        UNSUPPORTED_GRANT_TYPE ("The authorization grant type %s is not supported by the authorization server"),
        INVALID_GRANT_PASSWORD ("The authentication failed on user %s due to unknown login name or invalid password"),
        UNSUPPORTED_TOKEN_TYPE("The authentication token type %s is not supported by the authorization server"),
        UNRECOGNIZED_TOKEN("The authentication token %s with token type %s is invalid or expired");

        private String errDesc;

        private OAuthErrDescFormatter(String errDesc) {
            this.errDesc = errDesc;
        }

        @Override 
        public String toString() {
            return this.errDesc;
        }
    }
}
