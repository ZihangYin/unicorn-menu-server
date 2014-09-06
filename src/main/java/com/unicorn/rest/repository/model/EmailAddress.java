package com.unicorn.rest.repository.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.apache.commons.validator.routines.EmailValidator;

import com.unicorn.rest.repository.exception.ValidationException;

@EqualsAndHashCode
@ToString
@Getter
public class EmailAddress {
    
    @Nonnull private final String emailAddress;
    
    public EmailAddress(@Nullable String emailAddress) throws ValidationException {
        if (!validateEmailAddress(emailAddress)) {
            throw new ValidationException("Invalid email address: " + emailAddress);
        }
        this.emailAddress = emailAddress;
    }
    
    public static boolean validateEmailAddress(@Nullable String emailAddress){
        EmailValidator emailValidator = EmailValidator.getInstance();
        return emailValidator.isValid(emailAddress);
    }
}
