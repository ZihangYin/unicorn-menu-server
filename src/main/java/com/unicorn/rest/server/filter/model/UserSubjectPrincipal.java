package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

public class UserSubjectPrincipal implements SubjectPrincipal {

    private final @Nonnull Long userId;
    private final @Nonnull AuthenticationScheme authenticationScheme;
    
    public UserSubjectPrincipal(@Nonnull Long userId, @Nonnull AuthenticationScheme authenticationScheme) {
        this.userId = userId;
        this.authenticationScheme = authenticationScheme;
    }
    
    @Override
    public Long getPrincipal() {
        return this.userId;
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
        return String.valueOf(this.userId);
    }

}
