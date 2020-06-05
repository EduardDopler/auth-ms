package de.dopler.ms.login_server.utils;

import org.eclipse.jdt.annotation.NonNull;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;
import org.wildfly.security.password.util.ModularCrypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Bcrypt password generation and verification, inspired by
 * <a href="https://github.com/quarkusio/quarkus/blob/master/extensions/elytron-security-common/runtime/src/main/java/io/quarkus/elytron/security/common/BcryptUtil.java">Wildfly Elytron</a>.
 */
public final class PasswordHashUtils {

    private static final WildFlyElytronPasswordProvider PROVIDER = new WildFlyElytronPasswordProvider();
    private static final int ITERATION_COUNT = 10;

    private PasswordHashUtils() {
        // utility class
    }

    /**
     * Produce a Modular Crypt Format bcrypt hash of the given password, using a random
     * salt and the default {@link #ITERATION_COUNT} of {@value ITERATION_COUNT}.
     *
     * @param password the password to hash
     * @return the Modular Crypt Format bcrypt hash of the given password.
     */
    @NonNull
    public static String bcryptHash(@NonNull String password) {
        var salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
        new SecureRandom().nextBytes(salt);

        PasswordFactory passwordFactory;
        try {
            passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT,
                    PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "NoSuchAlgorithmException in PasswordHashUtils#bcryptHash: " + e.getMessage());
        }

        var iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(ITERATION_COUNT, salt);
        var encryptableSpec = new EncryptablePasswordSpec(password.toCharArray(),
                iteratedAlgorithmSpec);

        try {
            var original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);
            return ModularCrypt.encodeAsString(original);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(
                    "InvalidKeySpecException in PasswordHashUtils#bcryptHash: " + e.getMessage());
        }
    }

    /**
     * Verify if the given {@code guess} (unhashed password) corresponds to the bcrypt-hashed
     * {@code storedSecret}.
     *
     * @param storedSecret bcrypt-hashed secret, e.g. stored in your password store
     * @param guess unhashed password string to be verified against the stored (hashed) one
     * @return true if the guess was correct.
     */
    public static boolean verify(@NonNull String storedSecret, @NonNull String guess) {
        PasswordFactory passwordFactory;
        try {
            passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT,
                    PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "NoSuchAlgorithmException in PasswordHashUtils#verify: " + e.getMessage());
        }
        try {
            var rawPassword = ModularCrypt.decode(storedSecret);
            var password = (BCryptPassword) passwordFactory.translate(rawPassword);
            return passwordFactory.verify(password, guess.toCharArray());
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new RuntimeException(
                    "InvalidKey(Spec)Exception in PasswordHashUtils#verify: " + e.getMessage());
        }
    }
}
