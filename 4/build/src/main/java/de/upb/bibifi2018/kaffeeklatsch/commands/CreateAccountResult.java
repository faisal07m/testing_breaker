package de.upb.bibifi2018.kaffeeklatsch.commands;

import com.google.gson.JsonObject;
import okio.ByteString;

public class CreateAccountResult {

  private final ByteString privateKey;

  public CreateAccountResult(ByteString privateKey) {
    this.privateKey = privateKey;
  }

  public ByteString getPrivateKey() {
    return privateKey;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.CARD_PRIVATEKEY_KEY, this.privateKey.base64());
    return jsonObject;
  }

  public static CreateAccountResult fromJson(JsonObject jsonObject) {
    String privateKeyBase64 = jsonObject
        .getAsJsonPrimitive(JsonConstants.CARD_PRIVATEKEY_KEY)
        .getAsString();

    ByteString privateKey = ByteString.decodeBase64(privateKeyBase64);

    return new CreateAccountResult(privateKey);
  }
}
