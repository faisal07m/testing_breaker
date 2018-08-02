package de.upb.bibifi2018.kaffeeklatsch.exceptions;

public class BankingFailureException extends Exception {
  public BankingFailureException(String message) {
    super(message);
  }

  public BankingFailureException() {

  }
}
