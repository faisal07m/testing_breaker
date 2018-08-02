package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;

public class Balance extends AccountCommand {

  public Balance(String accountId) {
    super(accountId);
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.ACCOUNT_KEY, this.getAccountId());
    return jsonObject;
  }

  public static Balance fromJson(JsonObject commandJson) {
    String accountId = commandJson.getAsJsonPrimitive(JsonConstants.ACCOUNT_KEY).getAsString();
    return new Balance(accountId);
  }
}
