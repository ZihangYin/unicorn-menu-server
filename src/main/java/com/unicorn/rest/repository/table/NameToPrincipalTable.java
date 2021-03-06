package com.unicorn.rest.repository.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.StaleDataException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.Name;

@Singleton
public interface NameToPrincipalTable extends Table {
    public static final String NAME_TO_PRINCIPAL_TABLE_NAME = "NAME_TO_PRINCIPAL_TABLE";
    
    /**
     * Create name to principal mapping in the name_to_principal_table
     * The activate_in_epoch time is generated by server, and deactivate_in_epoch time is Long.MAX_VALUE
     * 
     * @param name @Nullable
     * @param principal @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if name is already mapped to existing principal
     * @throws RepositoryServerException internal server error
     */
    public void createNameForPrincipal(@Nullable Name name, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * 
     * Get the current name to principal from name_to_principal_table
     * create new name to principal mapping
     * and deactivate the old name to principal mapping
     * 
     * Note:
     * This might encounter some problems when there exists concurrent update requests from the same user/customer
     * However, we consider this as a rare case
     * 
     * If the create operation succeeds yet the deactivate operation failed, there is a 
     * chance that user/customer will have multiple names to log in. 
     * However, we can still figure out which one is the latest by checking the activate_in_epoch 
     * 
     * TODO: we reserve the ability to change name for now since tracking the historical mapping
     * between name and principal is a little bit complicated than we currently need
     * 
     * @param curName @Nullable
     * @param newName @Nullable
     * @param principal @Nullable
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if new name is already mapped to existing principal 
     * @throws ItemNotFoundException if name attempted to deleted mapped to a different principal or principal does not exist
     * @throws RepositoryServerException internal server error
     */
    public void updateNameForPrincipal(@Nullable Name curName, @Nullable Name newName, @Nullable Long principal) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the current principal for given name
     * If there is no active principal mapped to this name, throw ItemNotFoundException
     * 
     * @param name @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if no active principal mapped to this name
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getCurrentPrincipal(@Nullable Name name) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the principal for given name, which is active at given time
     * 
     * @param name @Nullable
     * @param activeTime @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if no active principal mapped to this name at activeTime
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getPrincipalAtTime(@Nullable Name name, @Nullable Long activeTime) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get name for principal.
     * 
     * Note:
     * Since principal-actiavet_in_epoch is global secondary index, there is currently no strong consistency.
     * meaning that there is a certain chance we will get stale data. 
     * However, we can check if the data is actually state by looking up the name again.
     * What is more, since failure can happen when user/customer updates name (successfully created the new one, yet failed to deactivate old one),
     * user/customer might have multiple names, but we can easily figure out the latest one 
     * by check the activate_in_epoch
     * 
     * @param principal @Nullable
     * @param checkStaleness @Nullable
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if the principal does not exist in the table
     * @throws StaleDataException if the server detected stale data for this request (only when checkStaleness flag is set true)
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull String getName(@Nullable Long principal, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException;
}
