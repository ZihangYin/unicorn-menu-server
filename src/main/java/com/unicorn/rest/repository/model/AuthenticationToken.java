package com.unicorn.rest.repository.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        
        public static AuthenticationTokenType fromString(@Nullable String tokenType) throws ValidationException {
            if (ACCESS_TOKEN.toString().equals(tokenType)) {
                return AuthenticationTokenType.ACCESS_TOKEN;
            } 
            throw new ValidationException(String.format("The authentication token type %s is not supported by the authorization server", tokenType));
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
        return new AuthenticationTokenBuilder().token(generateRandomToken());
    }

    public static AuthenticationTokenBuilder buildTokenBuilder(@Nonnull String token) {
        return new AuthenticationTokenBuilder().token(token);
    }
    
    public static AuthenticationToken updateTokenValue(AuthenticationToken currentAuthenticationToken) {
        return new AuthenticationToken(generateRandomToken(), currentAuthenticationToken.getTokenType(),
                currentAuthenticationToken.getIssuedAt(), currentAuthenticationToken.getExpireAt(), currentAuthenticationToken.getUserId());
    }
    
    /**
     * TODO: In order to prevent brute-force attack and better security, we should generate token value based on user_id 
     * or any salt generated on the server side and assign that token to the user. 
     * Still, we need to guarantee that the token value is globally unique.
     */
    private static String generateRandomToken() {        
        return UUIDGenerator.randomUUID().toString();
    }

    public static class AuthenticationTokenBuilder {

        private static final int DEFAULT_EXPIRATION_IN_HOURS = 7 * 24;

        private String token;
        private AuthenticationTokenType tokenType;
        private DateTime issuedAt = TimeUtils.getDateTimeNowInUTC();
        private DateTime expiredAt = issuedAt.plusHours(DEFAULT_EXPIRATION_IN_HOURS);
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
