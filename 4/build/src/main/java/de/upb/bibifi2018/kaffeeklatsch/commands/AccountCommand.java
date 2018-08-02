package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;

public abstract class AccountCommand {
  private final String accountId;

  protected AccountCommand(String accountId) {
    this.accountId = accountId;
  }

  public String getAccountId() {
    return accountId;
  }

  public abstract JsonObject toJson();
}
