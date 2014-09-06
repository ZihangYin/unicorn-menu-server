package com.unicorn.rest.activities.exception.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.unicorn.rest.activities.exception.InternalServerErrorException;
import com.unicorn.rest.activity.model.ErrorResponse;

@Provider
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException> {
    
    @Context
    private HttpHeaders headers;
    
    @Override
    public Response toResponse(InternalServerErrorException internalFailure) {
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .type(headers.getMediaType())
                .entity(new ErrorResponse(internalFailure.getClass().getSimpleName(), 
                        internalFailure.getErrorCode(), internalFailure.getErrorDescription()))
                .build();
    }
}
