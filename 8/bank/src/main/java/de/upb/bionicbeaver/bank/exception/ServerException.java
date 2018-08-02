package de.upb.bionicbeaver.bank.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base server exception class.
 *
 * @author Siddhartha Moitra
 */
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class ServerException extends RuntimeException {
    private final Error error;
}
