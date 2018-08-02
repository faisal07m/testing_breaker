package de.upb.bibifi2018.kaffeeklatsch.exceptions;

public class InvalidCardFileException extends Exception {
  public InvalidCardFileException(String message) {
    super(message);
  }

  public InvalidCardFileException(Throwable e) {
    super(e);
  }
}
