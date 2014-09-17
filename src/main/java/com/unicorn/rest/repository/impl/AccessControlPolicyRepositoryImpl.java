package com.unicorn.rest.repository.impl;

import java.util.List;

import javax.inject.Inject;

import com.unicorn.rest.repository.AccessControlPolicyRepository;
import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.table.AccessControlPolicyTable;

public class AccessControlPolicyRepositoryImpl implements AccessControlPolicyRepository {

    @Inject
    private AccessControlPolicyTable accessControlPolicyTable;
    
    @Inject
    public AccessControlPolicyRepositoryImpl(AccessControlPolicyTable accessControlPolicyTable) {
        this.accessControlPolicyTable = accessControlPolicyTable;
    }

    @Override
    public List<String> getAccessControlPolicy(String action,
            String resourceIdentifier) throws ValidationException,
            DuplicateKeyException, RepositoryServerException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
