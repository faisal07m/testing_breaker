package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CreateAccount extends AccountCommand {
  private static final BigDecimal LOWER_LIMIT = new BigDecimal("10.0");

  private final BigDecimal initialBalance;

  public CreateAccount(String accountId, BigDecimal initialBalance) {
    super(accountId);
    if (initialBalance.compareTo(LOWER_LIMIT) < 0) {
      throw new IllegalArgumentException("initialBalance mut be at least 10.0");
    }
    this.initialBalance = initialBalance.setScale(2, RoundingMode.UNNECESSARY);
  }

  public BigDecimal getInitialBalance() {
    return initialBalance;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.ACCOUNT_KEY, this.getAccountId());
    jsonObject.addProperty(JsonConstants.INITIAL_BALANCE_KEY, this.initialBalance);
    return jsonObject;
  }

  public static CreateAccount fromJson(JsonObject jsonObject) {
    String accountId = jsonObject.getAsJsonPrimitive(JsonConstants.ACCOUNT_KEY).getAsString();

    BigDecimal initialBalance = jsonObject
        .getAsJsonPrimitive(JsonConstants.INITIAL_BALANCE_KEY)
        .getAsBigDecimal()
        .setScale(2, RoundingMode.UNNECESSARY);

    return new CreateAccount(accountId, initialBalance);
  }
}
