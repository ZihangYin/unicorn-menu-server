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
public class DisplayName {
    /**
     * The regular expression pattern allows lower and upper case letters, dashes and underscores. 
     */
    public static Pattern userDisplayNamePattern = Pattern.compile("^[a-zA-Z_-]{3,25}$");
    public static Pattern customerDisplayNamePattern = Pattern.compile("^[a-zA-Z_-]{3,25}$");

    @Nonnull private final String displayName;

    public static DisplayName validateUserDisplayName(@Nonnull String userName) throws ValidationException {
        return new DisplayName(userName, userDisplayNamePattern);
    }

    public static DisplayName validateCustomerDisplayName(@Nonnull String customerName) throws ValidationException {
        return new DisplayName(customerName, customerDisplayNamePattern);
    }

    public static boolean validateDisplayName(@Nonnull String displayName, @Nonnull Pattern pattern){
        Matcher match = pattern.matcher(displayName);
        if(match.matches()){
            return true;
        }
        return false;
    }

    public DisplayName(@Nonnull String displayName) throws ValidationException {
        this.displayName = displayName;
    }
    
    private DisplayName(@Nonnull String displayName,  @Nonnull Pattern pattern) throws ValidationException {
        if (!validateDisplayName(displayName, pattern)) {
            String errMsg = "Invalid display name: %s. Only allows lower and upper case letters, dashes and underscores. "
                    + "The number of characters should be at least 3 and no more than 25";
            throw new ValidationException(String.format(errMsg, displayName));
        }
        this.displayName = displayName;
    }
}
