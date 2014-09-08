package com.unicorn.rest.utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.unicorn.rest.commons.ServiceConstants;
import com.unicorn.rest.repository.exception.ValidationException;


public class PasswordAuthenticationHelper {

    public static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    public static final int HASHING_ITERATIONS = 100;

    /**
     * The regular expression pattern requires the password to have 6 to 15 characters with at least one numeric digit, and at least one letter
     */
    private static Pattern passwordPattern = Pattern.compile("((?=.*\\d)(?=.*[a-zA-Z]).{6,15})");
    
    /**
     * This method is used to validate if the password is valid and strong enough
     * @param password @Nullable
     * @return
     */
    public static boolean validateStrongPassword(@Nonnull String password) {
        Matcher match = passwordPattern.matcher(password);
        if(match.matches()){
            return true;
        }
        return false;
    }
    
    /**
     * This method is used to verify if the password persisted in database matches one provided by user
     * @param userPassword @Nonnull
     * @param persistedPassword @Nonnull
     * @param persistedSalt @Nonnull
     * @return
     * @throws ValidationException 
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException

     */
    public static boolean authenticatePassword(@Nonnull String userPassword, @Nonnull ByteBuffer persistedPassword, @Nonnull ByteBuffer persistedSalt) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        return Arrays.equals(persistedPassword.array(), generateHashedPass(userPassword, persistedSalt));
    }

    /**
     * This method is used to generate a random salt
     * @return @Nonnull
     */
    public static @Nonnull ByteBuffer generateRandomSalt() {
        UUID salt = UUIDGenerator.randomUUID();
        ByteBuffer saltByteBuffer = ByteBuffer.wrap(new byte[16]);
        saltByteBuffer.putLong(salt.getMostSignificantBits());
        saltByteBuffer.putLong(salt.getLeastSignificantBits());

        // Prepare buffer to be read by resetting the positions to the beginning.
        saltByteBuffer.clear();
        return saltByteBuffer;
    }
    
    /**
     * This method is used to generate a new hashed password for plain-text password and salt provided by requester 
     * 
     * @param userPassword @Nullable
     * @param salt @Nonnull
     * @return @Nonnull
     * @throws ValidationException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public static @Nonnull ByteBuffer generateHashedPassWithSalt(@Nullable String userPassword, @Nonnull ByteBuffer salt) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        if (StringUtils.isBlank(userPassword)) {
            throw new ValidationException("Expecting non-null request paramter for generateHashedPassWithSalt, but received: userPassword=null");
        }
        return convertByteArrayToByteBuffer(generateHashedPass(userPassword, salt));
    }

    private static @Nonnull byte[] generateHashedPass(@Nonnull String userPassword, @Nonnull ByteBuffer salt) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return hashingPassword(HASHING_ITERATIONS, userPassword, salt.array());
    }

    private static @Nonnull byte[] hashingPassword(@Nonnull int numOfIterations, @Nonnull String password, @Nonnull byte[] salt) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // TODO: Monitoring time elapse for hashing process
        MessageDigest msgDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        msgDigest.update(salt);
        byte[] hashedPass = msgDigest.digest(password.getBytes(ServiceConstants.UTF_8_CHARSET));
        for (int i = 0; i < numOfIterations; i++) {
            msgDigest.reset();
            hashedPass = msgDigest.digest(hashedPass);
        }
        return hashedPass;
    }

    private static @Nonnull ByteBuffer convertByteArrayToByteBuffer(@Nonnull byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        // Prepare buffer to be read by resetting the positions to the beginning.
        byteBuffer.clear();
        return byteBuffer;
    }
}
