package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BalanceResult extends Result {
  private final BigDecimal balance;

  public BalanceResult(BigDecimal balance) {
    this.balance = balance.setScale(2, RoundingMode.UNNECESSARY);
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.BALANCE_KEY, this.balance);
    return jsonObject;
  }

  public static BalanceResult fromJson(JsonObject commandJson) {
    BigDecimal balance = commandJson
        .getAsJsonPrimitive(JsonConstants.BALANCE_KEY)
        .getAsBigDecimal();

    return new BalanceResult(balance);
  }
}
