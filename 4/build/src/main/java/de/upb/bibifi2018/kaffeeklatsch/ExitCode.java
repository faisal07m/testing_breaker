package de.upb.bibifi2018.kaffeeklatsch;

public enum ExitCode {
  SUCCESS(0),
  UNKNOWN_FAILURE(255),
  ARGUMENT_VIOLATION(255),
  DUPLICATE_ACCOUNT(255),
  DUPLICATE_CARD_FILE(255),
  BANKING_FAILURE(255),
  PROTOCOL_COMMUNICATION_FAILURE(63),
  TIMEOUT(63),
  INVALID_CARD(255);

  private final int exitCode;

  ExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public int getExitCode() {
    return exitCode;
  }
}
