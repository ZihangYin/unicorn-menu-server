package com.unicorn.rest.repository.model;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PrincipalAuthenticationInfo {
    
    @Getter @Nonnull private final Long principal;
    @Getter @Nonnull private final ByteBuffer password;
    @Getter @Nonnull private final ByteBuffer salt;
    
    public static PrincipalAuthorizationInfoBuilder buildPrincipalAuthorizationInfo() {
        return new PrincipalAuthorizationInfoBuilder();
    }
    
    public static class PrincipalAuthorizationInfoBuilder {
        private Long principal;
        private ByteBuffer password;
        private ByteBuffer salt;
        
        public PrincipalAuthorizationInfoBuilder() {}

        public PrincipalAuthorizationInfoBuilder principal(Long principal) {
            this.principal = principal;
            return this;
        }
        
        public PrincipalAuthorizationInfoBuilder password(ByteBuffer password) {
            this.password = password;
            return this;
        }
        
        public PrincipalAuthorizationInfoBuilder salt(ByteBuffer salt) {
            this.salt = salt;
            return this;
        }
        
        public PrincipalAuthenticationInfo build() {
            if (principal == null || password == null || salt == null) {
                throw new IllegalArgumentException("Failed while attempting to build user authorization info due to missing required parameters");
            }
            
            return new PrincipalAuthenticationInfo(principal, password, salt);
        }
    }
}
