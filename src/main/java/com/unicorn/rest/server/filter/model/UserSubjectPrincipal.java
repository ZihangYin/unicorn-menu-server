package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

public class UserSubjectPrincipal implements SubjectPrincipal {

    private final @Nonnull Long userId;
    private final @Nonnull AuthorizationScheme authenticationScheme;
    
    public UserSubjectPrincipal(@Nonnull Long userId, @Nonnull AuthorizationScheme authenticationScheme) {
        this.userId = userId;
        this.authenticationScheme = authenticationScheme;
    }
    
    @Override
    public Long getPrincipal() {
        return this.userId;
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
        return String.valueOf(this.userId);
    }

}
