package com.unicorn.rest.server.filter.model;

import java.security.Principal;

public interface SubjectPrincipal extends Principal {
    /**
     * Get this SubjectPrincipal's application-wide uniquely identifying principal.
     */
    public Object getPrincipal();    
    
    public AuthenticationScheme getAuthenticationScheme();
}