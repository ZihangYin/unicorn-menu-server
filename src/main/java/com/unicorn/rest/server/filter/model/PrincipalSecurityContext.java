package com.unicorn.rest.server.filter.model;

import java.util.List;

import javax.annotation.Nullable;
import javax.ws.rs.core.SecurityContext;

import com.unicorn.rest.activities.exception.AccessDeniedException;
import com.unicorn.rest.repository.AccessControlPolicyRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.server.filter.AccessControlPolicyEvaluator;

public class PrincipalSecurityContext implements SecurityContext {
 
    private AccessControlPolicyRepository accessControlPolicyRepository;
    private SubjectPrincipal subjectPrincipal;
    
    public PrincipalSecurityContext(SubjectPrincipal subjectPrincipal, AccessControlPolicyRepository accessControlPolicyRepository) {
        this.subjectPrincipal = subjectPrincipal;
        this.accessControlPolicyRepository = accessControlPolicyRepository;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getAuthenticationScheme() {
        return subjectPrincipal.getAuthenticationScheme().name();
    }
    
    public SubjectPrincipal getSubjectPrincipal() {
        return this.subjectPrincipal;
    }
    
    /**
     * Check if this Subject is permitted to perform an action or access a resource summarized by the
     * specified permission.
     */
    public boolean isPermitted(@Nullable String action, @Nullable String resourceIdentifier) {
        try {
            List<String> allowingConditions = accessControlPolicyRepository.getAccessControlPolicy(action, resourceIdentifier);
            return AccessControlPolicyEvaluator.evaluate(subjectPrincipal, allowingConditions);
            
        } catch (ValidationException | DuplicateKeyException
                | RepositoryServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean isPermitted(Permission permission) {
        return false;
    }

    /**
     * Asserts this Subject is permitted for the specified permission.
     * If not, an Exception will be thrown.
     */
    public void checkPermission(@Nullable String action, @Nullable String resourceIdentifier) throws AccessDeniedException {
        
    }
    
    public void checkPermission(Permission permission) throws AccessDeniedException {
        
    }
    
    /**
     * Check if this Subject has the specified role
     */
    public void checkRole(String roleIdentifier) throws AccessDeniedException {
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
     * Please use getSubjectPrincipal()
     */
    @Deprecated
    @Override
    public SubjectPrincipal getUserPrincipal() {
        return this.subjectPrincipal;
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
