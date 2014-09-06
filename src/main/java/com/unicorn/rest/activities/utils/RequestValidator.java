package com.unicorn.rest.activities.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.activities.exception.InvalidRequestException;

public class RequestValidator {

    public static void validateRequiredParameters(@Nonnull final MultivaluedMap<String, String> multiValuedParameters, @Nonnull String... requiredParas) 
            throws InvalidRequestException {

        for (String requiredPara : requiredParas) {
            String requiredParaVal = multiValuedParameters.getFirst(requiredPara);
            if (StringUtils.isBlank(requiredParaVal)) {
                throw new InvalidRequestException();
            }
        }
    }

    public static @Nonnull String validateRequiredParameter(@Nullable String requiredParaVal) 
            throws InvalidRequestException {

        if (StringUtils.isBlank(requiredParaVal)) {
            throw new InvalidRequestException();
        } else {
            return requiredParaVal;
        }
    }
}
