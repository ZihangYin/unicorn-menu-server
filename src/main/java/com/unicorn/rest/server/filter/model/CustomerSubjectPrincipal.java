package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

public class CustomerSubjectPrincipal implements SubjectPrincipal {

    private final @Nonnull Long customerId;
    private final @Nonnull AuthenticationScheme authenticationScheme;
    
    public CustomerSubjectPrincipal(@Nonnull Long customerId, @Nonnull AuthenticationScheme authenticationScheme) {
        this.customerId = customerId;
        this.authenticationScheme = authenticationScheme;
    }
    
    @Override
    public Long getPrincipal() {
        return this.customerId;
    }
    
    @Override
    public AuthenticationScheme getAuthenticationScheme() {
        return this.authenticationScheme;
    }
    
    /**
     * Please use getPrincipal()
     */
    @Deprecated
    @Override
    public String getName() {
        return String.valueOf(this.customerId);
    }

}
