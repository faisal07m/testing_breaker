package de.upb.bionicbeaver.atm.model;

/**
 * Enum describing the valid and known errors that server might send.
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
    PROTOCOL_ERROR
}
