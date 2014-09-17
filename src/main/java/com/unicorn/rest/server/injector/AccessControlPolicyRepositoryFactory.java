package com.unicorn.rest.server.injector;

import javax.inject.Inject;

import org.glassfish.hk2.api.Factory;

import com.unicorn.rest.repository.AccessControlPolicyRepository;
import com.unicorn.rest.repository.impl.AccessControlPolicyRepositoryImpl;
import com.unicorn.rest.repository.table.AccessControlPolicyTable;

public class AccessControlPolicyRepositoryFactory implements Factory<AccessControlPolicyRepository> {
 
    private final AccessControlPolicyRepository accessControlPolicyRepository;
    
    @Inject
    public AccessControlPolicyRepositoryFactory(AccessControlPolicyTable accessControlPolicyTable) {
        this.accessControlPolicyRepository = new AccessControlPolicyRepositoryImpl(accessControlPolicyTable);
    }
    
    @Override
    public AccessControlPolicyRepository provide() {
        return accessControlPolicyRepository;
    }

    @Override
    public void dispose(AccessControlPolicyRepository instance) {}
}
