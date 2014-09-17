package com.unicorn.rest.repository.table;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.server.filter.model.AccessControlPolicy;
import com.unicorn.rest.server.filter.model.Permission;

@Singleton
public interface AccessControlPolicyTable extends Table {

    public static final String ACCESS_CONTROL_POLICY_TABLE_NAME = "ACCESS_CONTROL_POLICY_TABLE";
    
    /**
     * Create access control policy for action on resource by requester
     * 
     * @param accessControlPolicy @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if access control policy with same action on same resource already exists
     * @throws RepositoryServerException internal server error
     */
    public void createAccessControlPolicy(@Nullable AccessControlPolicy accessControlPolicy) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Update existing access control policy for action on resource by requester
     * 
     * @param accessControlPolicy @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if access control policy attempted to update does not exist
     * @throws RepositoryServerException internal server error
     */
    public List<String> updateAccessControlPolicy(@Nullable AccessControlPolicy accessControlPolicy) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Revoke existing access control policy for action on resource by requester 
     * 
     * @param permission @Nullable
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if access control policy attempted to revoke does not exist
     * @throws RepositoryServerException internal server error
     */
    public void revokeAccessControlPolicy(@Nullable Permission permission) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get existing access control policy for action on resource
     * 
     * @param permission @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if access control policy attempted to get does not exist
     * @throws RepositoryServerException internal server error
     */
    public List<String> getAccessControlPolicy(@Nullable Permission permission) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
}
