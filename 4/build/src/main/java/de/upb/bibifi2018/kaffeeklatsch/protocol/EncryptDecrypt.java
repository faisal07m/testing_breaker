package de.upb.bibifi2018.kaffeeklatsch.protocol;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import java.util.Optional;
import okio.ByteString;

/**
 * Use NaCl primitives to encrypt and sign pieces of JSON.
 */
public class EncryptDecrypt {

  public static ByteString encryptSignJson(SimpleBox box, JsonObject json) {

    String serializedJson = json.toString();
    ByteString binaryJson = ByteString.encodeUtf8(serializedJson);
    return box.seal(binaryJson);
  }


  public static ByteString encryptSignJson(
      ByteString privateKey, ByteString publicKey, JsonObject json) {

    String serializedJson = json.toString();
    ByteString binaryJson = ByteString.encodeUtf8(serializedJson);
    SimpleBox simpleBox = new SimpleBox(publicKey, privateKey);
    return simpleBox.seal(binaryJson);
  }


  public static JsonObject verifyDecrypt(SimpleBox box, ByteString ciphertext
  ) throws ProtocolFailureException {

    Optional<ByteString> binaryJsonOptional = box.open(ciphertext);
    ByteString binaryJson = binaryJsonOptional
        .orElseThrow(() -> new ProtocolFailureException("broken message signature"));

    String jsonString = binaryJson.utf8();
    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
    return jsonObject;
  }

  public static JsonObject verifyDecrypt(
      ByteString privateKey, ByteString publicKey, ByteString ciphertext
  ) throws ProtocolFailureException {

    SimpleBox simpleBox = new SimpleBox(publicKey, privateKey);
    Optional<ByteString> binaryJsonOptional = simpleBox.open(ciphertext);
    ByteString binaryJson = binaryJsonOptional
        .orElseThrow(() -> new ProtocolFailureException("broken message signature"));

    String jsonString = binaryJson.utf8();
    JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
    return jsonObject;
  }

}
