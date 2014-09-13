package com.unicorn.rest.repository.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.unicorn.rest.repository.model.Name;

public class NameTest {

    @Test
    public void validateName() {
        
        assertFalse(Name.validateName("", Name.userNamePattern));
        assertFalse(Name.validateName(new String(), Name.userNamePattern));
        assertFalse(Name.validateName("abcd", Name.userNamePattern));
        assertFalse(Name.validateName("1234", Name.userNamePattern));
        assertFalse(Name.validateName("UserName", Name.userNamePattern));
        assertFalse(Name.validateName("username@gmail.com", Name.userNamePattern));
        assertFalse(Name.validateName("+11111111111", Name.userNamePattern));
        assertFalse(Name.validateName("usernameusername", Name.userNamePattern));
        
        assertTrue(Name.validateName("uname", Name.userNamePattern));
        assertTrue(Name.validateName("username", Name.userNamePattern));
        assertTrue(Name.validateName("user-name", Name.userNamePattern));
        assertTrue(Name.validateName("user_name", Name.userNamePattern));
        assertTrue(Name.validateName("user_name1", Name.userNamePattern));
        assertTrue(Name.validateName("user_name100", Name.userNamePattern));
        assertTrue(Name.validateName("12345", Name.userNamePattern));
    }
}