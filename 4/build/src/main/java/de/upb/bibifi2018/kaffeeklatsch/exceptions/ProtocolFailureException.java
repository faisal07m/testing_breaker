package de.upb.bibifi2018.kaffeeklatsch.exceptions;

public class ProtocolFailureException extends Exception {
  public ProtocolFailureException() {
  }

  public ProtocolFailureException(String message) {
    super(message);
  }

  public ProtocolFailureException(Throwable e) {
    super(e);
  }
}
