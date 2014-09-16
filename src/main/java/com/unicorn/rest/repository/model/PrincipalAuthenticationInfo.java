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
    
    public static PrincipalAuthenticationInfoBuilder buildPrincipalAuthenticationInfo() {
        return new PrincipalAuthenticationInfoBuilder();
    }
    
    public static class PrincipalAuthenticationInfoBuilder {
        private Long principal;
        private ByteBuffer password;
        private ByteBuffer salt;
        
        public PrincipalAuthenticationInfoBuilder() {}

        public PrincipalAuthenticationInfoBuilder principal(Long principal) {
            this.principal = principal;
            return this;
        }
        
        public PrincipalAuthenticationInfoBuilder password(ByteBuffer password) {
            this.password = password;
            return this;
        }
        
        public PrincipalAuthenticationInfoBuilder salt(ByteBuffer salt) {
            this.salt = salt;
            return this;
        }
        
        public PrincipalAuthenticationInfo build() {
            if (principal == null || password == null || salt == null) {
                throw new IllegalArgumentException("Failed while attempting to build user authentication info due to missing required parameters");
            }
            
            return new PrincipalAuthenticationInfo(principal, password, salt);
        }
    }
}
