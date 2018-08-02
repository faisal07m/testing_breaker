package de.upb.bibifi.verybest.common.exception;

public class CommunicationFailedException extends Exception {

    public CommunicationFailedException(Exception cause) {
        super(cause);
    }

    public CommunicationFailedException() {

    }
}
