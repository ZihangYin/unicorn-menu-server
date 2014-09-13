package com.unicorn.rest.repository.impl;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.repository.CustomerRepository;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.Name;
import com.unicorn.rest.repository.model.PrincipalAuthorizationInfo;
import com.unicorn.rest.repository.table.CustomerProfileTable;
import com.unicorn.rest.repository.table.NameToPrincipalTable;

public class CustomerRepositoryImpl implements CustomerRepository {

    private CustomerProfileTable customerProfileTable;
    private NameToPrincipalTable nameToPrincipalTable;

    @Inject
    public CustomerRepositoryImpl(CustomerProfileTable customerProfileTable, NameToPrincipalTable nameToPrincipalTable) {
        this.customerProfileTable = customerProfileTable;
        this.nameToPrincipalTable = nameToPrincipalTable;
    }

    @Override
    public Long getPrincipalForLoginName(String loginName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        if (StringUtils.isBlank(loginName)) {
            throw new ValidationException("Expecting non-null request paramter for getPrincipalForLoginName, but received: loginName=null");
        }
        Name customerName = new Name(loginName);
        return nameToPrincipalTable.getCurrentPrincipal(customerName);
    }

    @Override
    public PrincipalAuthorizationInfo getAuthorizationInfoForPrincipal(Long customerPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException {
        return customerProfileTable.getCustomerAuthorizationInfo(customerPrincipal);
    }

}
