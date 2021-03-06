package com.unicorn.rest.activity.model;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unicorn.rest.repository.model.AuthorizationToken;

@XmlRootElement(name="token")
@JsonInclude(value=Include.NON_NULL)

@EqualsAndHashCode
@NoArgsConstructor
public class TokenResponse {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_AT = "expire_at";
    private static final String PRINCIPAL = "principal";

    @JsonProperty(TOKEN_TYPE)
    @Getter @Setter private String tokenType;
    @JsonProperty(ACCESS_TOKEN)
    @Getter @Setter private String accessToken;
    @JsonProperty(EXPIRES_AT)
    @JsonInclude(value=Include.NON_DEFAULT)
    @Getter @Setter private Date expireAt;
    @JsonProperty(PRINCIPAL)
    @Getter @Setter private Long principal;

    public TokenResponse(@Nonnull AuthorizationToken authorizationToken) {
        this.tokenType = authorizationToken.getTokenType().toString();
        this.accessToken = authorizationToken.getToken();
        this.expireAt = authorizationToken.getExpireAt().toDate();
        this.principal = authorizationToken.getPrincipal();
    }

    @Override
    public String toString() {
        return "TokenResponse [tokenType=" + tokenType + ", accessToken="
                + accessToken + ", expireAt=" + expireAt 
                + ", principal=" + principal + "]";
    }

}
