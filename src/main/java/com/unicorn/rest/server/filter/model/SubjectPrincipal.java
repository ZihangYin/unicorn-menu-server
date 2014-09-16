package com.unicorn.rest.server.filter.model;

import java.security.Principal;

import com.unicorn.rest.server.filter.ActivitiesSecurityFilter.AuthorizationScheme;

public interface SubjectPrincipal extends Principal {
    /**
     * Get this SubjectPrincipal's application-wide uniquely identifying principal.
     */
    public Long getPrincipal();
    
    public PrincipalType getPrincipalType();  
    
    public AuthorizationScheme getAuthenticationScheme();
}
