package com.unicorn.rest.repository.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.StaleDataException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.UserName;

@Singleton
public interface UserNameToUserIdTable extends Table {
    
    /**
     * Create user_name to user_id mapping in the user_name_to_id_table
     * The activate_in_epoch time is generated by server, and deactivate_in_epoch time is Long.MAX_VALUE
     * 
     * @param userName
     * @param userId
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if user_name is already mapped to existing user_id
     * @throws RepositoryServerException internal server error
     */
    public void createUserNameForUserId(@Nullable UserName userName, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * TODO: we reserve the ability to change user_name for now since tracking the historical mapping
     * between user_name and user_id is a little bit complicated than we currently need
     * 
     * Get the current user_name to user_id from user_name_to_id_table
     * create new user_name to user_id mapping
     * and deactivate the old user_name to user_id mapping
     * 
     * Note:
     * This might encounter some problems when there exists concurrent update requests for the same user
     * However, we consider this as a rare case
     * 
     * If the create operation succeeds yet the deactivate operation failed, there is a 
     * chance that user will have multiple user_names to log in. 
     * However, we can still figure out which one is the latest by checking the activate_in_epoch 
     * 
     * @param curUserName
     * @param newUserName
     * @param userId
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if new user_name is already mapped to existing user_id 
     * @throws ItemNotFoundException if user_name attempted to deleted mapped to a different user_id or user_id does not exist
     * @throws RepositoryServerException internal server error
     */
    public void updateUserNameForUserId(@Nullable UserName curUserName, @Nullable UserName newUserName, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the current user_id for given user_name
     * If there is no active user_id mapped to this user_name, throw ItemNotFoundException
     * 
     * @param userName
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if no active user_id mapped to this user_name
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getCurrentUserId(@Nullable UserName userName) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get the user_id for given user_name, which is active at given time
     * 
     * @param userName
     * @param activeTime
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if no active user_id mapped to this user_name at activeTime
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getUserIdAtTime(@Nullable UserName userName, @Nullable Long activeTime) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get user_name for user_id.
     * 
     * Note:
     * Since user_id-actiavet_in_epoch is global secondary index, there is currently no strong consistency.
     * meaning that there is a certain chance we will get stale data. 
     * However, we can check if the data is actually state by looking up the user_name again.
     * What is more, since failure can happen when user updates user_name (successfully created the new one, yet failed to deactivate old one),
     * user might have multiple user_names, but we can easily figure out the latest one 
     * by check the activate_in_epoch
     * 
     * @param userId
     * @param checkStaleness
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if the user_id does not exist in the table
     * @throws StaleDataException if the server detected stale data for this request (only when checkStaleness flag is set true)
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull String getUserName(@Nullable Long userId, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException;
}
