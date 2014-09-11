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
import com.unicorn.rest.utils.AuthenticationSecretUtils;

public class AuthenticationSecretUtilsTest {
    
    @Test
    public void validateStrongPassword() throws ValidationException {
        assertFalse(AuthenticationSecretUtils.validateStrongSecret(new String()));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret(StringUtils.EMPTY));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret(StringUtils.SPACE));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret("1a2b"));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret("1a2b3"));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret("password"));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret("12345678"));
        assertFalse(AuthenticationSecretUtils.validateStrongSecret("abcdefgh12345678"));
        
        assertTrue(AuthenticationSecretUtils.validateStrongSecret("1a2b3c"));
    }
    
    @Test
    public void generateRandomSaltHappyCase() throws UnsupportedEncodingException {
        ByteBuffer saltOne = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer saltTwo = AuthenticationSecretUtils.generateRandomSalt();
        
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
        ByteBuffer saltOne = AuthenticationSecretUtils.generateRandomSalt();
        
        ByteBuffer hashedPasswordOne = AuthenticationSecretUtils.generateHashedSecretWithSalt(passwordOne, saltOne);
        ByteBuffer hashedPasswordDup = AuthenticationSecretUtils.generateHashedSecretWithSalt(passwordOne, saltOne);
        
        assertNotNull(hashedPasswordOne);
        assertThat(hashedPasswordOne, equalTo(hashedPasswordDup));
        
        String anotherPassword = "3c2b1a"; 
        ByteBuffer hashedPasswordTwo = AuthenticationSecretUtils.generateHashedSecretWithSalt(anotherPassword, saltOne);
        
        assertNotNull(hashedPasswordTwo);
        assertThat(hashedPasswordOne, not(equalTo(hashedPasswordTwo)));
        
        ByteBuffer anotherSalt = AuthenticationSecretUtils.generateRandomSalt();
        ByteBuffer hashedPasswordThree = AuthenticationSecretUtils.generateHashedSecretWithSalt(anotherPassword, anotherSalt);
        
        assertNotNull(hashedPasswordThree);
        assertThat(hashedPasswordOne, not(equalTo(hashedPasswordThree)));
        assertThat(hashedPasswordTwo, not(equalTo(hashedPasswordThree)));
    }
    
    @Test
    public void generateHashedPassWithSaltWithNullPassword() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        ByteBuffer saltOne = AuthenticationSecretUtils.generateRandomSalt();
        try{
            AuthenticationSecretUtils.generateHashedSecretWithSalt(null, saltOne);
        } catch(ValidationException error) {
            return;
        }
        fail("Failed while running generateHashedPassWithSaltWithNullPassword");
    }
    
    @Test
    public void authenticatePasswordHappyCase() throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        String passwordOne = "1a2b3c";  
        ByteBuffer saltOne = AuthenticationSecretUtils.generateRandomSalt();
        
        ByteBuffer hashedPasswordOne = AuthenticationSecretUtils.generateHashedSecretWithSalt(passwordOne, saltOne);
        assertTrue(AuthenticationSecretUtils.authenticateSecret(passwordOne, hashedPasswordOne, saltOne));
        
        String passwordTwo = "3c2b1a";
        assertFalse(AuthenticationSecretUtils.authenticateSecret(passwordTwo, hashedPasswordOne, saltOne));
        
        ByteBuffer saltTwo = AuthenticationSecretUtils.generateRandomSalt();
        assertFalse(AuthenticationSecretUtils.authenticateSecret(passwordOne, hashedPasswordOne, saltTwo));
        
    }
}
