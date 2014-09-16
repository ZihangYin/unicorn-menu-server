package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

import com.unicorn.rest.server.filter.ActivitiesSecurityFilter.AuthorizationScheme;

public class CustomerPrincipal implements SubjectPrincipal {

    private final @Nonnull Long customerId;
    private final @Nonnull PrincipalType principalType = PrincipalType.CUSTOMER;
    private final @Nonnull AuthorizationScheme authenticationScheme;
    
    public CustomerPrincipal(@Nonnull Long customerId, @Nonnull AuthorizationScheme authenticationScheme) {
        this.customerId = customerId;
        this.authenticationScheme = authenticationScheme;
    }
    
    @Override
    public Long getPrincipal() {
        return this.customerId;
    }
    
    @Override
    public PrincipalType getPrincipalType() {
        return principalType;
    }
    
    @Override
    public AuthorizationScheme getAuthenticationScheme() {
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
