package com.unicorn.rest.repository.table;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.unicorn.rest.repository.exception.DuplicateKeyException;
import com.unicorn.rest.repository.exception.ItemNotFoundException;
import com.unicorn.rest.repository.exception.RepositoryServerException;
import com.unicorn.rest.repository.exception.StaleDataException;
import com.unicorn.rest.repository.exception.ValidationException;
import com.unicorn.rest.repository.model.MobilePhone;

@Singleton
public interface MobilePhoneToUserIdTable extends Table {
    
    /**
     * Create mobile_phone to user_id mapping in the mobile_phone_to_id_table
     * The activate_in_epoch time is generated by server
     * 
     * @param phone
     * @param userId
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if mobile_phone is already mapped to existing user_id
     * @throws RepositoryServerException internal server error
     */
    public void createMobilePhoneForUserId(@Nullable MobilePhone mobilePhone, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, RepositoryServerException;
    
    /**
     * Get the current mobile_phone to user_id from mobile_phone_to_id_table
     * create new mobile_phone to user_id mapping
     * and delete the old mobile_phone to user_id mapping
     * 
     * Note:
     * This might encounter some problems when there exists concurrent update requests for the same user
     * However, we consider this as a rare case
     * 
     * If the create operation succeeds yet the delete operation failed, there is a 
     * chance that user will have multiple mobile_phone to log in. 
     * However, we can still figure out which one is the latest by checking the activate_in_epoch 
     * 
     * @param curPhone
     * @param newPhone
     * @param userId 
     * @throws ValidationException if request is invalid
     * @throws DuplicateKeyException if new mobile_phone is already mapped to existing user_id   
     * @throws ItemNotFoundException if mobile_phone attempted to deleted mapped to a different user_id or user_id does not exist
     * @throws RepositoryServerException internal server error
     */
    public void updateMobilePhoneForUserId(@Nullable MobilePhone curPhone, @Nullable MobilePhone newPhone, @Nullable Long userId) 
            throws ValidationException, DuplicateKeyException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get user_id for given mobile_phone
     * 
     * @param phoneNumber
     * @param countryCode
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if mobile_phone does not exist in the table
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull Long getUserId(@Nullable MobilePhone mobilePhone) 
            throws ValidationException, ItemNotFoundException, RepositoryServerException;
    
    /**
     * Get mobile_phone for user_id.
     * 
     * Note:
     * Since user_id-actiavet_in_epoch is global secondary index, there is currently no strong consistency.
     * meaning that there is a certain chance we will get stale data. 
     * However, we can check if the data is actually state by looking up the mobile phone again.
     * What is more, since failure can happen when user updates mobile_phone (successfully created the new one, yet failed to delete old one),
     * user might have multiple mobile_phones, but we can easily figure out the latest one 
     * by check the activate_in_epoch
     * 
     * @param userId
     * @return
     * @throws ValidationException if request is invalid
     * @throws ItemNotFoundException if the user_id does not exist in the table
     * @throws StaleDataException if the server detected stale data for this request (only when checkStaleness flag is set true)
     * @throws RepositoryServerException internal server error
     */
    public @Nonnull MobilePhone getMobilePhone(@Nullable Long userId, boolean checkStaleness) 
            throws ValidationException, ItemNotFoundException, StaleDataException, RepositoryServerException;
}
