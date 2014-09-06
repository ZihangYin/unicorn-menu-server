package com.unicorn.rest.utils;

import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import com.unicorn.rest.utils.SimpleFlakeKeyGenerator;

public class SimpleFlakeKeyGeneratorTest {
    
    @Test
    public void testGenerateKeyHappyCase() throws UnsupportedEncodingException {

        Assert.assertTrue(SimpleFlakeKeyGenerator.generateKeyByteBuffer().array().length == 8);
        Assert.assertFalse(SimpleFlakeKeyGenerator.generateKey() == SimpleFlakeKeyGenerator.generateKey());
    }

    @Test
    public void testGenerateAThounsandKeyHappyCase() {
        int ITERATIONS = 1_000;
        HashSet<Long> results = new HashSet<>(ITERATIONS, 1_000);
        for (int i = 0; i < ITERATIONS; i++) {
            results.add(SimpleFlakeKeyGenerator.generateKey());
        }

        // Since a Set can't have duplicates there must be exactly 1000
        Assert.assertEquals(ITERATIONS, results.size());
    }
    
    @Test
    public void testGenerateKeyInvalidCurTimestamp() {
        
        try {
            SimpleFlakeKeyGenerator.generateKey(0);
        } catch (IllegalArgumentException error1) {
            try {
                SimpleFlakeKeyGenerator.generateKey(Long.MAX_VALUE);
            } catch (IllegalArgumentException error2) {
                return;
            }
        }
        fail();
    }
}
