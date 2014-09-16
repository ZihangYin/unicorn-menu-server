package com.unicorn.rest.server.filter.model;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

public class PrincipalSecurityContext implements SecurityContext {
    
    @Inject 
    Provider<UriInfo> uriInfo;
    
    private SubjectPrincipal subjectPrincipal;
    
    public PrincipalSecurityContext(SubjectPrincipal subjectPrincipal) {
        this.subjectPrincipal = subjectPrincipal;
    }
    
    @Override
    public SubjectPrincipal getUserPrincipal() {
        return this.subjectPrincipal;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return subjectPrincipal.getAuthenticationScheme().name();
    }
    
    /**
     * Check if this Subject is permitted to perform an action or access a resource summarized by the
     * specified permission.
     */
    public boolean isPermitted(Permission permission) {
        return false;
    }

    /**
     * Asserts this Subject is permitted for the specified permission.
     * If not, an Exception will be thrown.
     */
    public void checkPermission(Permission permission) throws Exception {
        
    }
    
    /**
     * Check if this Subject has the specified role
     */
    public void checkRole(String roleIdentifier) throws Exception {
    }
    
    /**
     * Asserts this Subject has the specified role. 
     * If not, an Exception will be thrown.
     *
     * @param roleIdentifier the application-specific role identifier (usually a role id or role name ).
     * @throws org.apache.shiro.authz.AuthorizationException
     *          if this Subject does not have the role.
     */
    public boolean hasRole(String roleIdentifier) {
        return false;
    }

    /**
     * Please use hasRole(String roleIdentifier) or checkRole(String roleIdentifier)
     */
    @Deprecated
    @Override
    public boolean isUserInRole(String roleIdentifier) {
        return false;
    }
}
