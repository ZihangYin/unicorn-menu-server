package com.unicorn.rest.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeUtils {
    
    public static final DateTimeZone UTC_TIME_ZONE = DateTimeZone.UTC;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss z").withZone(UTC_TIME_ZONE);
    
    
    /**
     * Convert a number attribute value in the format seconds.subseconds to 
     * a time value in milliseconds without the loss of precision that could
     * occur when converting to double first
     *
     * @param @Nullable seoncdsWithSubSeconds @Nullable
     * @return Long @Nullable
     * @throws NumberFormatException
     */
    public static @Nullable Long convertToTimeInMills(@Nullable String seoncdsWithSubSeconds) throws NumberFormatException {
        if (seoncdsWithSubSeconds == null) {
            return null;
        }
        
        boolean negative = false;
        if (seoncdsWithSubSeconds.startsWith("-")) {
            negative = true;
            seoncdsWithSubSeconds = seoncdsWithSubSeconds.substring(1);
        }
        int decimalIndex = seoncdsWithSubSeconds.indexOf('.');
        
        String seconds, subseconds;
        if (decimalIndex < 0) {
            seconds = seoncdsWithSubSeconds;
            subseconds = "";
        } else {
            seconds = seoncdsWithSubSeconds.substring(0, decimalIndex);
            subseconds = seoncdsWithSubSeconds.substring(decimalIndex + 1);
        }
        
        long mills = 0;
        if (seconds.length() > 0 ) {
            mills += Long.parseLong(seconds) * 1000;
        } 
        switch(subseconds.length()) {
        case 0: break;
        case 1: mills += Long.parseLong(subseconds) * 100; break;
        case 2: mills += Long.parseLong(subseconds) * 10; break;
        case 3:
        default:
            mills += Long.parseLong(subseconds.substring(0, 3)); 
            break;
        }
        return negative? -mills : mills;
    }
    
    /**
     * Convert a time value in milliseconds to a number attribute value in the format 
     * seconds.subseconds without the loss of precision that could result from
     * converting to milliseconds first
     * @param mills @Nullable
     * @return String @Nullable
     */
    public static @Nullable String convertToSeoncdsWithSubSeconds(@Nullable Long mills) {
        if (mills == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        if (mills < 0) {
            builder.append("-");
            mills = -mills;
        }
        
        builder.append(mills / 1000);
        long subSeconds = mills % 1000;
        if (subSeconds > 0) {
            builder.append('.');
            for (long div = 100; subSeconds > 0 ; div /= 10) {
                builder.append(subSeconds / div);
                subSeconds %= div;
            }
        }
        
        return builder.toString();
    }
    
    /**
     * Get the current DateTime in UTC time zone
     * @return @Nonnull
     */
    public static @Nonnull DateTime getDateTimeNowInUTC() {
        return new DateTime(UTC_TIME_ZONE);
    }
    
    /**
     * Get the current epoch time in UTC time zone
     * @return @Nonnull
     */
    public static @Nonnull Long getEpochTimeNowInUTC() {
        return getDateTimeNowInUTC().getMillis();
    }
    
    /**
     * Convert epoch time to date time in UTC time zone
     * @param timeInMills
     * @return @Nonnull
     */
    public static @Nonnull DateTime convertToDateTimeInUTCWithEpochTime(@Nonnull Long timeInMills) {
        return new DateTime(timeInMills, UTC_TIME_ZONE);
    }
}
