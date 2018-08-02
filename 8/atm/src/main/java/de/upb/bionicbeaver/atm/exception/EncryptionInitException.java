package de.upb.bionicbeaver.atm.exception;

/**
 * Exception thrown if the {@link de.upb.bionicbeaver.atm.encryption.EncryptionManager} fails to initialize itself.
 */
public class EncryptionInitException extends RuntimeException {

    public EncryptionInitException(String message) {
        super(message);
    }

    public EncryptionInitException(Throwable cause) {
        super(cause);
    }
}
