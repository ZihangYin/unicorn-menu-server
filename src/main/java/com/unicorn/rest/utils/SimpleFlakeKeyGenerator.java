package com.unicorn.rest.utils;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SimpleFlakeKeyGenerator {

    /**
     * SIMPLE-FLAKE
     * 
     * http://engineering.custommade.com/simpleflake-distributed-id-generation-for-the-lazy/
     * 
     * For now, each of our unique IDs should consist of: 
     * 42 bits for time in milliseconds (gives us roughly 140 years with a custom epoch)
     * 22 bits for completely random value 
     * 
     * NOTE: 
     * If we DO get a collision, since the keys are exactly same, this key should go to the exact same shard as the other one, 
     * resulting in a DuplicateKey error. 
     * When this extremely rare event happens, just re-insert the same item with a freshly generated key.
     * 
     */ 

    private static final long EPOCH_TIME_IN_MILLS = new DateTime(2014, 7, 7, 0, 0, DateTimeZone.UTC).getMillis(); //1404691200000L
    private static final long MAX_TIME_IN_MILLS = (1L << 42) - 1; //4398046511103L
    private static final int RIGHT_MOST_BITS_FOR_RANDOM = 22;
    private static final int LEFT_MOST_BITS_FOR_TIMESTAMP = 42;
    
    /**
     * Locally generate a universally unique key
     * @return @Nonnull
     */
    public static @Nonnull ByteBuffer generateKeyByteBuffer() {
        return ByteBuffer.allocate(8).putLong(SimpleFlakeKeyGenerator.generateKey());
    }
    
    /**
     * Locally generate a universally unique key
     * @return @Nonnull
     */
    public static @Nonnull Long generateKey() {
        /**
         * TODO: clock adjustment (We DO NOT consider this as a critical problems at this moment, so ignore this for now.)
         * System.currentTimeMillis() is not monotonic. It is based on system time, and hence can be subject to variation either way (forward or backward) 
         * in the case of clock adjustments.
         * 
         * For more details about System.currentTimeMillis() and System.nanoTime(),  
         * please refer to: https://blogs.oracle.com/dholmes/entry/inside_the_hotspot_vm_clocks
         */
        long curTimestamp = System.currentTimeMillis();
        return generateKey(curTimestamp);
    }
    
    /*
     * This method is protected for unit test
     */
    protected static @Nonnull Long generateKey(long curTimestamp) {
        if (curTimestamp < EPOCH_TIME_IN_MILLS || curTimestamp > MAX_TIME_IN_MILLS) {
            /*
             *  The current time cannot be less than the customized EPOCH_TIME_IN_MILLS nor larger than the MAX_TIME_IN_MILLS
             *  In latter case, we will end up having duplicate keys
             */
            throw new IllegalArgumentException("Invalid system clock " + new DateTime(curTimestamp));
        }

        long epochTimestamp = curTimestamp - EPOCH_TIME_IN_MILLS;
        long shiftedTimestamp = epochTimestamp << RIGHT_MOST_BITS_FOR_RANDOM;

        long random = ThreadLocalRandom.current().nextLong() >>> LEFT_MOST_BITS_FOR_TIMESTAMP;
        return shiftedTimestamp | random;
    }
    
}