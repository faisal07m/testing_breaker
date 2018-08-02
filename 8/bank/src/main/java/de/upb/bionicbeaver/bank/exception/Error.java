package de.upb.bionicbeaver.bank.exception;

/**
 * Enum describing the valid and know errors that could happen while processing a transaction.
 *
 * @author Siddhartha Moitra
 */
public enum Error {
    USER_NOT_PRESENT,
    DUPLICATE_USER,
    USER_AUTHENTICATION_FAILED,
    INVALID_REQUEST,
    INSUFFICIENT_BALANCE,
    BAD_REQUEST,
    PROTOCOL_ERROR,
    REQUEST_REPLAY
}
