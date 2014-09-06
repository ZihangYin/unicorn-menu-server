package com.unicorn.rest.activities.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.unicorn.rest.activities.exception.BadRequestException;
import com.unicorn.rest.activity.model.ErrorResponse;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(BadRequestException badRequest) {

        return  Response.status(Status.BAD_REQUEST)
                .type(headers.getMediaType())
                .entity(new ErrorResponse(badRequest.getErrorType(), 
                        badRequest.getErrorCode(), badRequest.getErrorDescription()))
                .build();
    }
}
