package com.unicorn.rest.repository.table;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.DisplayName;
import com.unicorn.rest.repository.model.PrincipalAuthorizationInfo;

@Singleton
public interface CustomerProfileTable extends Table {
    
    public static final String CUSTOMER_PROFILE_TABLE_NAME = "CUSTOMER_PROFILE_TABLE";
    /**
     * Create new customer with required minimum parameters
     *  
     * TODO: we reserve the ability to create new customer for the business purpose
     *  
     * @param customerPrincipal @Nullable
     * @param customerDisplayName @Nullable
     * @param password @Nullable
     * @param salt @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if the customer_principal already exists
     * @throws RepositoryServerException internal server error
     */
    public Long createCustomer(@Nullable Long customerPrincipal, @Nullable DisplayName customerDisplayName, @Nullable ByteBuffer password, @Nullable ByteBuffer salt) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Build customer_authorization_info from attributes, which contains customer_display_name, hashed password and salt.
     * 
     * @param customerPrincipal @Nullable
     * @return 
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if customer_principal does not exist
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull PrincipalAuthorizationInfo getCustomerAuthorizationInfo(@Nullable Long customerPrincipal) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
}
