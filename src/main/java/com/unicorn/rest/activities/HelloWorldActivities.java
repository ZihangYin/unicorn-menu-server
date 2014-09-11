package com.unicorn.rest.activities;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/hello")
public class HelloWorldActivities {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response sayHello() {
        return Response.ok("Hello World").build();        
    }
}