package de.upb.bibifi.verybest.common.exception;

public class OperationFailedException extends Exception {
    public OperationFailedException(Exception e) {
        super(e);
    }

    public OperationFailedException() {

    }
}
