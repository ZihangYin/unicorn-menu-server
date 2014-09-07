package com.unicorn.rest.activities.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.repository.exception.ValidationException;

public class RequestValidator {

    public static void validateRequiredParameters(@Nonnull final MultivaluedMap<String, String> multiValuedParameters, @Nonnull String... requiredParas) 
            throws ValidationException {

        StringBuilder errDescBuilder = null;
        for (String requiredPara : requiredParas) {
            String requiredParaVal = multiValuedParameters.getFirst(requiredPara);
            if (StringUtils.isBlank(requiredParaVal)) {
                if (errDescBuilder != null) {
                    errDescBuilder.append(", ");
                } else {
                    errDescBuilder = new StringBuilder();
                }
                errDescBuilder.append(requiredPara);
            }
        }

        if (errDescBuilder != null) {
            throw new ValidationException(String.format("The request is missing required parameters: %s", errDescBuilder.toString()));
        }
    }

    public static @Nonnull String validateRequiredParameter(@Nonnull String requiredPara, @Nullable String requiredParaVal) 
            throws ValidationException {

        if (StringUtils.isBlank(requiredParaVal)) {
            throw new ValidationException(String.format("The request is missing required parameters: %s", requiredPara));
        } else {
            return requiredParaVal;
        }
    }
}
