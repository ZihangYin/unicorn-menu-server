package com.unicorn.rest.repository.impl.dynamodb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;

public class DynamoAttributeValueUtils {
    
    public static @Nonnull IllegalArgumentException wrongAttrTypeException(@Nonnull String expectedType, @Nonnull AttributeValue attrValue) {
        throw new IllegalArgumentException("expected: " + expectedType + ", but got: " + attrValue);
    }

    public static @Nonnull IllegalArgumentException invalidAttrValueException(@Nonnull String attrName, @Nonnull IllegalArgumentException error) {
        throw new IllegalArgumentException("invalid value for " + attrName + " attribute: " + error.getMessage(), error);
    }

    public static @Nonnull IllegalArgumentException missingRequiredAttrException(@Nonnull String attrName) {
        throw new IllegalArgumentException("missing required attribute: " + attrName);
    }

    public static @Nonnull AttributeValueUpdate delete() {
        return new AttributeValueUpdate().withAction(AttributeAction.DELETE);
    }

    public static @Nonnull AttributeValueUpdate updateTo(@Nonnull AttributeValue attrValue) {
        return new AttributeValueUpdate().withValue(attrValue).withAction(AttributeAction.PUT);
    }

    public static @Nonnull AttributeValueUpdate atomicAdd(@Nonnull AttributeValue attrValue) {
        return new AttributeValueUpdate().withValue(attrValue).withAction(AttributeAction.ADD);
    }

    public static @Nonnull ExpectedAttributeValue expectEmpty() {
        return new ExpectedAttributeValue(false);
    }

    public static @Nonnull ExpectedAttributeValue expectEqual(@Nonnull AttributeValue attrValue) {
        return new ExpectedAttributeValue().withValue(attrValue);
    }

    public static @Nonnull ExpectedAttributeValue expectCompare(@Nonnull ComparisonOperator comparisonOperator, @Nonnull AttributeValue attrValue) {
        return new ExpectedAttributeValue().withValue(attrValue).withComparisonOperator(comparisonOperator);
    }

    public static @Nonnull ExpectedAttributeValue expectCompare(@Nonnull ComparisonOperator comparisonOperator, @Nonnull List<AttributeValue> attrValues) {
        return new ExpectedAttributeValue().withAttributeValueList(attrValues).withComparisonOperator(comparisonOperator);
    }

    public static @Nullable String asString(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        if (attrValue == null) {
            return null;
        } 
        String strValue = attrValue.getS();
        if (strValue == null) {
            throw wrongAttrTypeException("String", attrValue);
        }
        return strValue;
    }
    
    public static @Nullable List<String> asStringSet(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        if (attrValue == null) {
            return null;
        } 
        List<String> strValues = attrValue.getSS();
        if (strValues == null) {
            throw wrongAttrTypeException("StringSet", attrValue);
        }
        return strValues;
    }

    public static @Nullable Double asDouble(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        String numberValue = asNumber(attrValue);
        if (numberValue == null) {
            return null;
        }
        try {
            return Double.parseDouble(numberValue);
        } catch(NumberFormatException error) {
            throw wrongAttrTypeException("Double", attrValue);
        }
    }

    public static @Nullable Long asLong(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        String numberValue = asNumber(attrValue);
        if (numberValue == null) {
            return null;
        }
        try {
            return Long.parseLong(numberValue);
        } catch(NumberFormatException error) {
            throw wrongAttrTypeException("Long", attrValue);
        }
    }

    public static @Nullable Integer asInteger(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        String numberValue = asNumber(attrValue);
        if (numberValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(numberValue);
        } catch(NumberFormatException error) {
            throw wrongAttrTypeException("Integer", attrValue);
        }
    }

    private static @Nullable String asNumber(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        if (attrValue == null) {
            return null;
        } 
        String numberValue = attrValue.getN();
        if (numberValue == null) {
            throw wrongAttrTypeException("Number", attrValue);
        }
        return numberValue;
    }

    public static @Nullable ByteBuffer asByteBuffer(@Nullable AttributeValue attrValue) throws IllegalArgumentException {
        if (attrValue == null) {
            return null;
        } 
        ByteBuffer byteBufferValue = attrValue.getB();
        if (byteBufferValue == null) {
            throw wrongAttrTypeException("Binary", attrValue);
        }
        return byteBufferValue;
    }
    
    public static @Nullable DateTime asDateTime(@Nullable AttributeValue attrValue, @Nonnull DateTimeFormatter dateTimeFormatter) throws IllegalArgumentException {
        if (attrValue == null) {
            return null;
        } 
        String strValue = asString(attrValue);
        try {
            return DateTime.parse(strValue, dateTimeFormatter);
        } catch (IllegalArgumentException error) {
            throw wrongAttrTypeException("DateTime", attrValue);
        }
    }

    public static @Nullable String getStringValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asString(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }
    
    public static @Nullable List<String> getStringSetValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asStringSet(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }

    public static @Nullable Double getDoubleValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asDouble(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }

    public static @Nullable Long getLongValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asLong(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }

    public static @Nullable Integer getIntegerValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asInteger(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }

    public static @Nullable ByteBuffer getByteBufferValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        try { 
            return asByteBuffer(attributes.get(attrName));
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }
    
    public static @Nullable DateTime getDateTimeValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName, @Nonnull DateTimeFormatter dateTimeFormatter) 
            throws IllegalArgumentException {
        try { 
            return asDateTime(attributes.get(attrName), dateTimeFormatter);
        } catch (IllegalArgumentException error) {
            throw invalidAttrValueException(attrName, error);
        }
    }

    public static @Nonnull String getRequiredStringValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        String strValue = getStringValue(attributes, attrName);
        if (strValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return strValue;
    }
    
    public static @Nonnull List<String> getRequiredStringSetValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        List<String> strValues = getStringSetValue(attributes, attrName);
        if (strValues == null) {
            throw missingRequiredAttrException(attrName);
        }
        return strValues;
    }

    public static @Nonnull Double getRequiredDoubleValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        Double numberValue = getDoubleValue(attributes, attrName);
        if (numberValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return numberValue;
    }

    public static @Nonnull Long getRequiredLongValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        Long numberValue = getLongValue(attributes, attrName);
        if (numberValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return numberValue;
    }

    public static @Nonnull Integer getRequiredIntegerValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        Integer numberValue = getIntegerValue(attributes, attrName);
        if (numberValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return numberValue;
    }

    public static @Nonnull ByteBuffer getRequiredByteBufferValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName) throws IllegalArgumentException {
        ByteBuffer byteBufferValue = getByteBufferValue(attributes, attrName);
        if (byteBufferValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return byteBufferValue;
    }
    
    public static @Nonnull DateTime getRequiredDateTimeValue(@Nonnull Map<String, AttributeValue> attributes, @Nonnull String attrName, @Nonnull DateTimeFormatter dateTimeFormatter) 
            throws IllegalArgumentException {
        DateTime dateTimeValue = getDateTimeValue(attributes, attrName, dateTimeFormatter);
        if (dateTimeValue == null) {
            throw missingRequiredAttrException(attrName);
        }
        return dateTimeValue;
    }

    public static @Nullable AttributeValue stringAttrValue(@Nullable String str) {
        return (StringUtils.isBlank(str) ? null : new AttributeValue().withS(str));
    }

    public static @Nullable AttributeValue stringSetAttrValue(@Nullable List<String> strs) {
        return (CollectionUtils.sizeIsEmpty(strs) ? null : new AttributeValue().withSS(strs));
    }
    
    public static @Nullable AttributeValue numberAttrValue(@Nullable Number number) {
        return (number == null ? null : new AttributeValue().withN(number.toString()));
    }

    public static @Nullable AttributeValue byteBufferAttrValue(@Nullable ByteBuffer byteBuffer) {
        return (byteBuffer == null ? null : new AttributeValue().withB(byteBuffer));
    }
    
    public static @Nullable AttributeValue dateTimeAttrValue(@Nullable DateTime dateTime, @Nonnull DateTimeFormatter dateTimeFormatter) {
        return (dateTime == null ? null : new AttributeValue().withS(dateTime.toString(dateTimeFormatter)));
    }
}
