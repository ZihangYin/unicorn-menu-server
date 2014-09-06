package com.unicorn.rest.repository.model;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAuthorizationInfo {
    
    @Getter @Nonnull private final Long userId;
    @Getter @Nonnull private final ByteBuffer password;
    @Getter @Nonnull private final ByteBuffer salt;
    
    public static UserAuthorizationInfoBuilder buildUserAuthorizationInfo() {
        return new UserAuthorizationInfoBuilder();
    }
    
    public static class UserAuthorizationInfoBuilder {
        private Long userId;
        private ByteBuffer password;
        private ByteBuffer salt;
        
        public UserAuthorizationInfoBuilder() {}

        public UserAuthorizationInfoBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public UserAuthorizationInfoBuilder password(ByteBuffer password) {
            this.password = password;
            return this;
        }
        
        public UserAuthorizationInfoBuilder salt(ByteBuffer salt) {
            this.salt = salt;
            return this;
        }
        
        public UserAuthorizationInfo build() {
            if (userId == null || password == null || salt == null) {
                throw new IllegalArgumentException("Failed while attempting to build user authorization info due to missing required parameters");
            }
            
            return new UserAuthorizationInfo(userId, password, salt);
        }
    }
}
