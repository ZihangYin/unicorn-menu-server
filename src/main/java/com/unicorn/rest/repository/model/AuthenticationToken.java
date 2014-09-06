package com.unicorn.rest.repository.model;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.joda.time.DateTime;

import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.utils.TimeUtils;
import com.unicorn.rest.utils.UUIDGenerator;

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationToken {

    public enum AuthenticationTokenType {
        ACCESS_TOKEN("bearer");

        private String tokenType;
        private AuthenticationTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        @Override
        public String toString() {
            return tokenType;
        }
        
        public static AuthenticationTokenType fromString(String tokenType) throws ValidationException {
            if (ACCESS_TOKEN.toString().equals(tokenType)) {
                return AuthenticationTokenType.ACCESS_TOKEN;
            } 
            throw new ValidationException("The authentication token type %s is invalid");
        }
    }
    
    // TODO: In future, we might consider using Optional<T> for optional parameters
    @Getter @Nonnull private final String token;
    @Getter @Nonnull private final AuthenticationTokenType tokenType;
    @Getter @Nonnull private final DateTime issuedAt;
    @Getter @Nonnull private final DateTime expireAt;
    @Getter @Nonnull private final Long userId;

    public static AuthenticationToken generateAccessTokenForUser(@Nonnull Long userId) {
        return generateTokenBuilder().tokenType(AuthenticationTokenType.ACCESS_TOKEN).userId(userId).build();
    }
    
    public static AuthenticationTokenBuilder generateTokenBuilder() {
        return new AuthenticationTokenBuilder().token(UUIDGenerator.randomUUID().toString());
    }

    public static AuthenticationTokenBuilder buildTokenBuilder(@Nonnull String token) {
        return new AuthenticationTokenBuilder().token(token);
    }
    
    public static AuthenticationToken updateTokenValue(AuthenticationToken currentAuthenticationToken) {
        return new AuthenticationToken(UUIDGenerator.randomUUID().toString(), currentAuthenticationToken.getTokenType(),
                currentAuthenticationToken.getIssuedAt(), currentAuthenticationToken.getExpireAt(), currentAuthenticationToken.getUserId());
    }

    public static class AuthenticationTokenBuilder {

        private static final int DEFAULT_EXPIRATION_IN_HOURS = 24 * 60;

        private String token;
        private AuthenticationTokenType tokenType;
        private DateTime issuedAt = TimeUtils.getDateTimeNowInUTC();
        private DateTime expiredAt = issuedAt.plusMinutes(DEFAULT_EXPIRATION_IN_HOURS);
        private Long userId;

        public AuthenticationTokenBuilder() {}

        private AuthenticationTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthenticationTokenBuilder tokenType(AuthenticationTokenType tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public AuthenticationTokenBuilder issuedAt(DateTime issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public AuthenticationTokenBuilder expiredAt(DateTime expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public AuthenticationTokenBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AuthenticationToken build() {
            if (token == null || tokenType == null || issuedAt == null || expiredAt == null || userId == null) {
                throw new IllegalArgumentException("Failed while attempting to build authentication token due to missing required parameters");
            }
            return new AuthenticationToken(token, tokenType, issuedAt, expiredAt, userId);
        }
    }
}
