package com.unicorn.rest.repository.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.repository.exception.ValidationException;

@EqualsAndHashCode
@ToString
@Getter
public class UserName {
    /**
     * TODO: We might consider requiring to have at least one letter in the user_name
     * 
     * The regular expression pattern allows lower case alphanumeric characters, dashes and underscores. 
     * In case we want to support upper case characters in future, the regular expression should be ^[a-zA-Z0-9_-]{3,15}$
     */
    private static Pattern userNamePattern = Pattern.compile("^[a-z0-9_-]{5,15}$");

    @Nonnull private final String userName;

    public UserName(@Nullable String userName) throws ValidationException {
        if (!validateUserName(userName)) {
            String errMsg = "Invalid user name: %s. Only allows lower case alphanumeric characters, dashes and underscores. "
                    + "The number of characters should be more than 5 and less than 15";
            throw new ValidationException( String.format(errMsg, userName));
        }
        this.userName = userName;
    }

    public static boolean validateUserName(@Nullable String userName){
        if (!StringUtils.isBlank(userName)) {
            Matcher match = userNamePattern.matcher(userName);
            if(match.matches()){
                return true;
            }
        }
        return false;
    }
    
}
