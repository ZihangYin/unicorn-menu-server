package com.unicorn.rest.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTests {

    private static void convertToTimeInMills(Long expectedTimeInMills, String seoncdsWithSubSeconds) {
        //positive case
        assertEquals(expectedTimeInMills, TimeUtils.convertToTimeInMills(seoncdsWithSubSeconds));
        //negative case
        assertEquals(Long.valueOf(-expectedTimeInMills), 
                TimeUtils.convertToTimeInMills("-" + seoncdsWithSubSeconds));
    }

    @Test
    public void testConvertToTimeInMills() {
        assertEquals(null, TimeUtils.convertToTimeInMills(null));
        convertToTimeInMills(0L, "0");
        convertToTimeInMills(0L, "0.");
        convertToTimeInMills(0L, "0.0");
        convertToTimeInMills(0L, "0.00");
        convertToTimeInMills(0L, "0.000");
        convertToTimeInMills(0L, "0.0000");
        convertToTimeInMills(0L, ".0");
        convertToTimeInMills(0L, ".00");
        convertToTimeInMills(0L, ".000");
        convertToTimeInMills(0L, ".0000");
        convertToTimeInMills(1000L, "1");
        convertToTimeInMills(11000L, "11");
        convertToTimeInMills(100L, "0.1");
        convertToTimeInMills(100L, "0.10");
        convertToTimeInMills(100L, "0.100");
        convertToTimeInMills(100L, "0.1000");
        convertToTimeInMills(110L, "0.11");
        convertToTimeInMills(111L, "0.111");
        convertToTimeInMills(111L, "0.1111");
        convertToTimeInMills(10L, "0.01");
        convertToTimeInMills(10L, "0.010");
        convertToTimeInMills(10L, "0.0100");
        convertToTimeInMills(11L, "0.011");
        convertToTimeInMills(11L, "0.0111");
        convertToTimeInMills(1L, "0.001");
        convertToTimeInMills(1L, "0.0010");
        convertToTimeInMills(1L, "0.0011");
        convertToTimeInMills(2000L, "2");
        convertToTimeInMills(3000L, "3");
        convertToTimeInMills(4000L, "4");
        convertToTimeInMills(5000L, "5");
        convertToTimeInMills(6000L, "6");
        convertToTimeInMills(7000L, "7");
        convertToTimeInMills(8000L, "8");
        convertToTimeInMills(9000L, "9");
        convertToTimeInMills(2L, ".002");
        convertToTimeInMills(3L, ".003");
        convertToTimeInMills(4L, ".004");
        convertToTimeInMills(5L, ".005");
        convertToTimeInMills(6L, ".006");
        convertToTimeInMills(7L, ".007");
        convertToTimeInMills(8L, ".008");
        convertToTimeInMills(9L, ".009");
    }

    private static void convertToSeoncdsWithSubSeconds(String expectedSeoncdsWithSubSeconds, Long timeInMills) {
        //positive case
        assertEquals(expectedSeoncdsWithSubSeconds, TimeUtils.convertToSeoncdsWithSubSeconds(timeInMills));
        //negative case
        assertEquals("-" + expectedSeoncdsWithSubSeconds, TimeUtils.convertToSeoncdsWithSubSeconds(-timeInMills));
    }

    @Test
    public void testConvertToSeoncdsWithSubSeconds() {
        assertEquals(null, TimeUtils.convertToSeoncdsWithSubSeconds(null));
        assertEquals("0", TimeUtils.convertToSeoncdsWithSubSeconds(0L));
        convertToSeoncdsWithSubSeconds("1", 1000L);
        convertToSeoncdsWithSubSeconds("11", 11000L);
        convertToSeoncdsWithSubSeconds("1.1", 1100L);
        convertToSeoncdsWithSubSeconds("1.11", 1110L);
        convertToSeoncdsWithSubSeconds("1.111", 1111L);
        convertToSeoncdsWithSubSeconds("0.1", 100L);
        convertToSeoncdsWithSubSeconds("0.11", 110L);
        convertToSeoncdsWithSubSeconds("0.111", 111L);
        convertToSeoncdsWithSubSeconds("0.01", 10L);
        convertToSeoncdsWithSubSeconds("0.001", 1L);
    }
}
