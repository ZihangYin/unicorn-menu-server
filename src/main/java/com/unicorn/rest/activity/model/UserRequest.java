package com.unicorn.rest.activity.model;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement(name="user")
@JsonInclude(value=Include.NON_NULL)

@EqualsAndHashCode
@NoArgsConstructor
public class UserRequest {
    
    public static final String USER_PRINCIPAL = "user_principal";
    public static final String USER_NAME = "user_name";
    public static final String EMAIL_ADDRESS = "email_address";
    public static final String MOBILE_PHONE = "mobile_phone";
    public static final String PASSWORD = "password";
    public static final String USER_DISPLAY_NAME = "user_display_name";

    @JsonProperty(USER_PRINCIPAL)
    @Getter @Setter private Long userPrincipal;
    @JsonProperty(USER_NAME)
    @Getter @Setter private String userName;
    @JsonProperty(EMAIL_ADDRESS)
    @Getter @Setter private String emailAddress;
    @JsonProperty(MOBILE_PHONE)
    @Getter @Setter private String mobilePhone;
    @JsonProperty(PASSWORD)
    @JsonIgnore
    @Getter @Setter private String password;
    @JsonProperty(USER_DISPLAY_NAME)
    @Getter @Setter private String userDisplayName;
    
    @Override
    public String toString() {
        return "UserRequest [userPrincipal=" + userPrincipal + ", userName=" + userName
                + ", emailAddress=" + emailAddress + ", mobilePhone="
                + mobilePhone + ", password=******, userDisplayName="
                + userDisplayName + "]";
    }
    
}
