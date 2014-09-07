package com.unicorn.rest.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.utils.PasswordAuthenticationHelper;

public class UserPassAuthenticationHelperTest {
    
    @Test
    public void validateStrongPassword() throws ValidationException {
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword(null));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword(new String()));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword(StringUtils.EMPTY));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword(StringUtils.SPACE));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword("1a2b"));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword("1a2b3"));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword("password"));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword("12345678"));
        assertFalse(PasswordAuthenticationHelper.validateStrongPassword("abcdefgh12345678"));
        
        assertTrue(PasswordAuthenticationHelper.validateStrongPassword("1a2b3c"));
    }
    
    @Test
    public void generateRandomSaltHappyCase() throws UnsupportedEncodingException {
        ByteBuffer saltOne = PasswordAuthenticationHelper.generateRandomSalt();
        ByteBuffer saltTwo = PasswordAuthenticationHelper.generateRandomSalt();
        
        assertNotNull(saltOne);
        assertNotNull(saltTwo);
        
        UUID uuidOne = new UUID(saltOne.getLong(), saltOne.getLong());
        UUID uuidTwo = new UUID(saltTwo.getLong(), saltTwo.getLong());
        
        assertThat(saltOne.array(), not(equalTo(saltTwo.array())));
        assertNotEquals(uuidOne, uuidTwo);
    }
    
    @Test
    public void generateHashedPassWithSaltHappyCase() throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String passwordOne = "1a2b3c";  
        ByteBuffer saltOne = PasswordAuthenticationHelper.generateRandomSalt();
        
        ByteBuffer hashedPasswordOne = PasswordAuthenticationHelper.generateHashedPassWithSalt(passwordOne, saltOne);
        ByteBuffer hashedPasswordDup = PasswordAuthenticationHelper.generateHashedPassWithSalt(passwordOne, saltOne);
        
        assertNotNull(hashedPasswordOne);
        assertThat(hashedPasswordOne, equalTo(hashedPasswordDup));
        
        String anotherPassword = "3c2b1a"; 
        ByteBuffer hashedPasswordTwo = PasswordAuthenticationHelper.generateHashedPassWithSalt(anotherPassword, saltOne);
        
        assertNotNull(hashedPasswordTwo);
        assertThat(hashedPasswordOne, not(equalTo(hashedPasswordTwo)));
        
        ByteBuffer anotherSalt = PasswordAuthenticationHelper.generateRandomSalt();
        ByteBuffer hashedPasswordThree = PasswordAuthenticationHelper.generateHashedPassWithSalt(anotherPassword, anotherSalt);
        
        assertNotNull(hashedPasswordThree);
        assertThat(hashedPasswordOne, not(equalTo(hashedPasswordThree)));
        assertThat(hashedPasswordTwo, not(equalTo(hashedPasswordThree)));
    }
    
    @Test
    public void generateHashedPassWithSaltWithNullPassword() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        ByteBuffer saltOne = PasswordAuthenticationHelper.generateRandomSalt();
        try{
            PasswordAuthenticationHelper.generateHashedPassWithSalt(null, saltOne);
        } catch(ValidationException error) {
            return;
        }
        fail("Failed while running generateHashedPassWithSaltWithNullPassword");
    }
    
    @Test
    public void authenticatePasswordHappyCase() throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String passwordOne = "1a2b3c";  
        ByteBuffer saltOne = PasswordAuthenticationHelper.generateRandomSalt();
        
        ByteBuffer hashedPasswordOne = PasswordAuthenticationHelper.generateHashedPassWithSalt(passwordOne, saltOne);
        assertTrue(PasswordAuthenticationHelper.authenticatePassword(passwordOne, hashedPasswordOne, saltOne));
        
        String passwordTwo = "3c2b1a";
        assertFalse(PasswordAuthenticationHelper.authenticatePassword(passwordTwo, hashedPasswordOne, saltOne));
        
        ByteBuffer saltTwo = PasswordAuthenticationHelper.generateRandomSalt();
        assertFalse(PasswordAuthenticationHelper.authenticatePassword(passwordOne, hashedPasswordOne, saltTwo));
        
    }
}
