package de.upb.bibifi2018.kaffeeklatsch.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import okio.ByteString;
import org.junit.jupiter.api.Test;

public class EncryptDecryptTest {

  private final ByteString bankPrivKey = Keys.generatePrivateKey();
  private final ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);
  private final ByteString atmPrivKey = Keys.generatePrivateKey();
  private final ByteString atmPubKey = Keys.generatePublicKey(atmPrivKey);


  @Test
  public void testSignEncrypt() throws ProtocolFailureException {
    JsonObject json = new JsonObject();
    json.addProperty("a", "b");
    json.addProperty("b", "c");
    ByteString ciphertext = EncryptDecrypt.encryptSignJson(bankPrivKey, atmPubKey, json);

    JsonObject jsonObject = EncryptDecrypt.verifyDecrypt(atmPrivKey, bankPubKey, ciphertext);

    assertEquals(json, jsonObject);
  }

  @Test
  public void testSignEncryptBoxes() throws ProtocolFailureException {
    JsonObject json = new JsonObject();
    json.addProperty("a", "b");
    json.addProperty("b", "c");
    SimpleBox fromBank = new SimpleBox(atmPubKey, bankPrivKey);
    SimpleBox fromAtm = new SimpleBox(bankPubKey, atmPrivKey);
    ByteString ciphertext = EncryptDecrypt.encryptSignJson(fromBank, json);

    JsonObject jsonObject = EncryptDecrypt.verifyDecrypt(fromAtm, ciphertext);

    assertEquals(json, jsonObject);
  }
}
