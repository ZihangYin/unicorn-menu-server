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
public class AuthorizationToken {

    public enum AuthorizationTokenType {
        ACCESS_TOKEN("bearer");

        private String tokenType;
        private AuthorizationTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        @Override
        public String toString() {
            return tokenType;
        }
        
        public static AuthorizationTokenType fromString(@Nullable String tokenType) throws ValidationException {
            if (ACCESS_TOKEN.toString().equals(tokenType)) {
                return AuthorizationTokenType.ACCESS_TOKEN;
            } 
            throw new ValidationException(String.format("The authorization token type %s is not supported by the authorization server", tokenType));
        }
    }
    
    // TODO: In future, we might consider using Optional<T> for optional parameters
    @Getter @Nonnull private final String token;
    @Getter @Nonnull private final AuthorizationTokenType tokenType;
    @Getter @Nonnull private final DateTime issuedAt;
    @Getter @Nonnull private final DateTime expireAt;
    @Getter @Nonnull private final Long principal;

    public static AuthorizationToken generateAccessToken(@Nonnull Long principal) throws ValidationException {
        return generateTokenBuilder().tokenType(AuthorizationTokenType.ACCESS_TOKEN).principal(principal).build();
    }
    
    public static AuthorizationTokenBuilder generateTokenBuilder() {
        return new AuthorizationTokenBuilder().token(generateRandomToken());
    }

    public static AuthorizationTokenBuilder buildTokenBuilder(@Nonnull String token) {
        return new AuthorizationTokenBuilder().token(token);
    }
    
    public static AuthorizationToken updateTokenValue(AuthorizationToken currentAuthorizationToken) {
        return new AuthorizationToken(generateRandomToken(), currentAuthorizationToken.getTokenType(),
                currentAuthorizationToken.getIssuedAt(), currentAuthorizationToken.getExpireAt(), 
                currentAuthorizationToken.getPrincipal());
    }
    
    /**
     * TODO: In order to prevent brute-force attack and better security, we should generate token value based on client_id 
     * or any salt generated on the server side and assign that token to the client. 
     * Still, we need to guarantee that the token value is globally unique.
     * 
     * For now, we require the client to provide both the principal and token to relieve this problem.
     */
    private static String generateRandomToken() {        
        return UUIDGenerator.randomUUID().toString();
    }

    public static class AuthorizationTokenBuilder {

        private static final int DEFAULT_EXPIRATION_IN_HOURS = 7 * 24;

        private String token;
        private AuthorizationTokenType tokenType;
        private DateTime issuedAt = TimeUtils.getDateTimeNowInUTC();
        private DateTime expiredAt = issuedAt.plusHours(DEFAULT_EXPIRATION_IN_HOURS);
        private Long principal;

        public AuthorizationTokenBuilder() {}

        private AuthorizationTokenBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthorizationTokenBuilder tokenType(AuthorizationTokenType tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public AuthorizationTokenBuilder issuedAt(DateTime issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public AuthorizationTokenBuilder expiredAt(DateTime expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public AuthorizationTokenBuilder principal(Long principal) {
            this.principal = principal;
            return this;
        }

        public AuthorizationToken build() throws ValidationException {
            if (token == null || tokenType == null || issuedAt == null || expiredAt == null || principal == null) {
                throw new ValidationException("Failed while attempting to build authorization token due to missing required parameters");
            }
            return new AuthorizationToken(token, tokenType, issuedAt, expiredAt, principal);
        }
    }
}
