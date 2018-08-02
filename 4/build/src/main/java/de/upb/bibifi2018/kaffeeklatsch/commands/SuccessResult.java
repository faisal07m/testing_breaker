package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;

public class SuccessResult extends Result {
  private final boolean success;

  public SuccessResult(boolean success) {
    this.success = success;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.SUCCESS_KEY, this.success);
    return jsonObject;
  }

  public static SuccessResult fromJson(JsonObject json) {
    boolean success = json.getAsJsonPrimitive("success").getAsBoolean();
    return new SuccessResult(success);
  }

  public boolean isSuccess() {
    return success;
  }
}
