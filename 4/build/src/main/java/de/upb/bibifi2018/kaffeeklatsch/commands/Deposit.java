package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Deposit extends AccountCommand {
  private final BigDecimal amount;

  public Deposit(String accountId, BigDecimal amount) {
    super(accountId);
    this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    if (this.amount.signum() < 1) {
      throw new IllegalArgumentException("deposit must be > 0.00");
    }
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.ACCOUNT_KEY, this.getAccountId());
    jsonObject.addProperty(JsonConstants.DEPOSIT_KEY, this.amount);
    return jsonObject;
  }

  public static Deposit fromJson(JsonObject commandJson) {
    String accountId = commandJson.getAsJsonPrimitive(JsonConstants.ACCOUNT_KEY).getAsString();
    BigDecimal withdrawAmount = commandJson
        .getAsJsonPrimitive(JsonConstants.DEPOSIT_KEY)
        .getAsBigDecimal();

    return new Deposit(accountId, withdrawAmount);
  }
}
