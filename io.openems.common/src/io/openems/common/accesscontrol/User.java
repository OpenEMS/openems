package io.openems.common.accesscontrol;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * This class represents a user which is in a normal context a human being with feelings and stuff.
 * A user has among other things a username and password which allows him to log in. Every user is assigned to exactly one {@link Role}
 * which is holding the {@link ExecutePermission} and {@link io.openems.common.channel.AccessMode}
 *
 * @author Sebastian.Walbrun
 */
public class User {

    /**
     * statics
     */
    private final static int KEY_LENGTH = 256;

    /**
     * private final static int SALT_LENGTH = 32;
     */
    private final static int ITERATIONS = 10;

    private String id;
    private String username;
    private String description;
    private String email;
    private final byte[] password;
    private final byte[] salt;
    private RoleId role;


    public User(String id, String username, String description, String email, String passwordAsBase64, String saltAsBase64, RoleId role) {
        this(id, username, description, email, Base64.getDecoder().decode(passwordAsBase64), Base64.getDecoder().decode(saltAsBase64), role);
    }


    public User(String id, String username, String description, String email, byte[] password, byte[] salt, RoleId role) {
        this.id = id;
        this.username = username;
        this.description = description;
        this.email = email;
        this.password = password;
        this.salt = salt;
        this.role = role;
    }

    String getId() {
        return id;
    }

    String getUsername() {
        return username;
    }

    /**
     * Validates the hashed password
     *
     * @param password the hashed password
     * @return true if success false otherwise
     */
    boolean validateHashedPassword(String password) {
        if (this.password == null || this.salt == null) {
            // no password existing -> allow access
            return true;
        }
        return Arrays.equals(password.getBytes(), this.password);
    }

    /**
     * Validates the plain password
     *
     * @param password the plain password
     * @return true if success false otherwise
     */
    boolean validatePlainPassword(String password) {
        if (this.password == null || this.salt == null) {
            // no password existing -> allow access
            return true;
        }
        byte[] hashedPassword = hashPassword(password, this.salt);
        return Arrays.equals(hashedPassword, this.password);
    }

    private static byte[] hashPassword(final String password, final byte[] salt) {
        return hashPassword(password.toCharArray(), salt);
    }

    /**
     * Source: https://www.owasp.org/index.php/Hashing_Java
     *
     * @param password
     * @param salt
     * @return
     */
    private static byte[] hashPassword(final char[] password, final byte[] salt) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(password, salt, User.ITERATIONS, User.KEY_LENGTH);
            SecretKey key = skf.generateSecret(spec);
            byte[] res = key.getEncoded();
            return res;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    RoleId getRoleId() {
        return role;
    }

    public void setRoleId(RoleId roleId) {
        this.role = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
