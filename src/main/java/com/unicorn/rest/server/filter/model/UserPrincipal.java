package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

import com.unicorn.rest.server.filter.ActivitiesSecurityFilter.AuthorizationScheme;

public class UserPrincipal implements SubjectPrincipal {

    private final @Nonnull Long userId;
    private final @Nonnull PrincipalType principalType = PrincipalType.USER;
    private final @Nonnull AuthorizationScheme authenticationScheme;
    
    public UserPrincipal(@Nonnull Long userId, @Nonnull AuthorizationScheme authenticationScheme) {
        this.userId = userId;
        this.authenticationScheme = authenticationScheme;
    }
    
    @Override
    public Long getPrincipal() {
        return this.userId;
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
        return String.valueOf(this.userId);
    }

}
