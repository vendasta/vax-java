package com.vendasta.vax;

public class CredentialsException extends RuntimeException {
    /**
     * Because we inherit from Serializable, this is a requirement. It is used
     * during deserialization to ensure that the implementation matches that
     * used to serialize.
     */
    private static final long serialVersionUID = 1L;

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