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
public class Name {
    /**
     * The regular expression pattern allows lower case alphanumeric characters, dashes and underscores. 
     * In case we want to support upper case characters in future, the regular expression should be ^[a-zA-Z0-9_-]{3,15}$
     */
    public static Pattern userNamePattern = Pattern.compile("^[a-z0-9_-]{5,15}$");
    public static Pattern customerNamePattern = Pattern.compile("^[a-z0-9_-]{5,15}$");

    @Nonnull private final String name;

    public static Name validateUserName(@Nonnull String userName) throws ValidationException {
       return new Name(userName, userNamePattern);
    }
    
    public static Name validateCustomerName(@Nonnull String customerName) throws ValidationException {
        return new Name(customerName, customerNamePattern);
    }
    
    public static boolean validateName(@Nonnull String name, @Nonnull Pattern pattern){
        Matcher match = pattern.matcher(name);
        if(match.matches()){
            return true;
        }
        return false;
    }
    
    public Name(@Nonnull String name) throws ValidationException {
        this.name = name;
    }
    
    private Name(@Nonnull String name, @Nonnull Pattern pattern) throws ValidationException {
        if (!validateName(name, pattern)) {
            String errMsg = "Invalid name: %s. Only allows lower case alphanumeric characters, dashes and underscores. "
                    + "The number of characters should be at least 5 and no more than 15";
            throw new ValidationException(String.format(errMsg, name));
        }
        this.name = name;
    }
}
