package de.upb.bionicbeaver.atm.exception;

/**
 * Thrown in case the client could not connect to the socket successfully.
 *
 * @author Siddhartha Moitra
 */
public class ProtocolException extends RuntimeException {
    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
