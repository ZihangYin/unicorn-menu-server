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


public class AuthenticationSecretUtils {

    public static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    public static final int HASHING_ITERATIONS = 100;

    /**
     * The regular expression pattern requires the secret to have 6 to 15 characters with at least one numeric digit, and at least one letter
     */
    private static Pattern defaultSecretPattern = Pattern.compile("((?=.*\\d)(?=.*[a-zA-Z]).{6,15})");
    
    /**
     * This method is used to validate if the secret is valid and strong enough
     * @param secret @Nonnull
     * @return
     */
    public static boolean validateStrongSecret(@Nonnull String secret) {
        return validateStrongSecret(secret, defaultSecretPattern);
    }
    
    /**
     * This method is used to validate if the secret is valid and strong enough
     * @param secret @Nonnull
     * @param secretPattern @Nonnull
     * @return
     */
    public static boolean validateStrongSecret(@Nonnull String secret, @Nonnull Pattern secretPattern) {
        Matcher match = secretPattern.matcher(secret);
        if(match.matches()){
            return true;
        }
        return false;
    }
    
    /**
     * This method is used to verify if the secret persisted in database matches one provided by user
     * @param userSecret @Nonnull
     * @param persistedSecret @Nonnull
     * @param persistedSalt @Nonnull
     * @return
     * @throws ValidationException 
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException

     */
    public static boolean authenticateSecret(@Nonnull String userSecret, @Nonnull ByteBuffer persistedSecret, @Nonnull ByteBuffer persistedSalt) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        return Arrays.equals(persistedSecret.array(), generateHashedSecret(userSecret, persistedSalt));
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
     * This method is used to generate a new hashed secret for plain-text secret and salt provided by requester 
     * 
     * @param userSecret @Nullable
     * @param salt @Nonnull
     * @return @Nonnull
     * @throws ValidationException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    public static @Nonnull ByteBuffer generateHashedSecretWithSalt(@Nullable String userSecret, @Nonnull ByteBuffer salt) 
            throws ValidationException, UnsupportedEncodingException, NoSuchAlgorithmException {
        if (StringUtils.isBlank(userSecret)) {
            throw new ValidationException("Expecting non-null request paramter for generateHashedSecretWithSalt, but received: userSecret=null");
        }
        return convertByteArrayToByteBuffer(generateHashedSecret(userSecret, salt));
    }

    private static @Nonnull byte[] generateHashedSecret(@Nonnull String userSecret, @Nonnull ByteBuffer salt) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return hashingSecret(HASHING_ITERATIONS, userSecret, salt.array());
    }

    private static @Nonnull byte[] hashingSecret(@Nonnull int numOfIterations, @Nonnull String secret, @Nonnull byte[] salt) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // TODO: Monitoring time elapse for hashing process
        MessageDigest msgDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        msgDigest.update(salt);
        byte[] hashedSecret = msgDigest.digest(secret.getBytes(ServiceConstants.UTF_8_CHARSET));
        for (int i = 0; i < numOfIterations; i++) {
            msgDigest.reset();
            hashedSecret = msgDigest.digest(hashedSecret);
        }
        return hashedSecret;
    }

    private static @Nonnull ByteBuffer convertByteArrayToByteBuffer(@Nonnull byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        // Prepare buffer to be read by resetting the positions to the beginning.
        byteBuffer.clear();
        return byteBuffer;
    }
}
