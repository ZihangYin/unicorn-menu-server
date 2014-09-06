package com.unicorn.rest.repository.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UserNameTest {

    @Test
    public void validateUserName() {
        
        assertFalse(UserName.validateUserName(null));
        assertFalse(UserName.validateUserName(""));
        assertFalse(UserName.validateUserName(new String()));
        assertFalse(UserName.validateUserName("abcd"));
        assertFalse(UserName.validateUserName("1234"));
        assertFalse(UserName.validateUserName("UserName"));
        assertFalse(UserName.validateUserName("username@gmail.com"));
        assertFalse(UserName.validateUserName("+11111111111"));
        assertFalse(UserName.validateUserName("usernameusername"));
        
        assertTrue(UserName.validateUserName("uname"));
        assertTrue(UserName.validateUserName("username"));
        assertTrue(UserName.validateUserName("user-name"));
        assertTrue(UserName.validateUserName("user_name"));
        assertTrue(UserName.validateUserName("user_name1"));
        assertTrue(UserName.validateUserName("user_name100"));
        assertTrue(UserName.validateUserName("12345"));
    }
}