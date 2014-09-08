package com.unicorn.rest.repository.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.unicorn.rest.repository.exception.ValidationException;

@EqualsAndHashCode
@ToString
@Getter
public class UserDisplayName {
    /**
     * The regular expression pattern allows lower and upper case letters, dashes and underscores. 
     */
    private static Pattern userDisplayNamePattern = Pattern.compile("^[a-zA-Z_-]{3,25}$");

    @Nonnull private final String userDisplayName;

    public UserDisplayName(@Nonnull String userDisplayName) throws ValidationException {
        if (!validateUserName(userDisplayName)) {
            String errMsg = "Invalid user display name: %s. Only allows lower and upper case letters, dashes and underscores. "
                    + "The number of characters should be at least 3 and no more than 25";
            throw new ValidationException(String.format(errMsg, userDisplayName));
        }
        this.userDisplayName = userDisplayName;
    }

    public static boolean validateUserName(@Nonnull String userDisplayName){
        Matcher match = userDisplayNamePattern.matcher(userDisplayName);
        if(match.matches()){
            return true;
        }
        return false;
    }
    
}
