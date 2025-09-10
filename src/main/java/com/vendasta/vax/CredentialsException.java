package com.vendasta.vax;

/**
 * Exception thrown when there are issues with credential management.
 * 
 * <p>This includes problems with loading credentials, token refresh failures,
 * and authentication errors.
 */
public class CredentialsException extends RuntimeException {
    /**
     * Because we inherit from Serializable, this is a requirement. It is used
     * during deserialization to ensure that the implementation matches that
     * used to serialize.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new credentials exception with the specified message.
     * 
     * @param message the error message
     */
    public CredentialsException(String message) {
        super(message);
    }

    public CredentialsException(String message, Throwable t) {
        super(message, t);
    }

    public CredentialsException(Throwable t) {
        super(t);
    }
}