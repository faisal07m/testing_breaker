package de.upb.bibifi2018.kaffeeklatsch.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import okio.ByteString;
import org.junit.jupiter.api.Test;

public class ChallengeProtocolTest {
  private final ByteString bankPrivKey = Keys.generatePrivateKey();
  private final ByteString bankPubKey = Keys.generatePublicKey(bankPrivKey);
  private final ByteString atmPrivKey = Keys.generatePrivateKey();
  private final ByteString atmPubKey = Keys.generatePublicKey(atmPrivKey);
  private final SimpleBox bankKeyPair = new SimpleBox(atmPubKey, bankPrivKey);
  private final SimpleBox atmKeyPair = new SimpleBox(bankPubKey, atmPrivKey);

  @Test
  void testHello() throws ProtocolFailureException, ReplayAttackException {
    ChallengeProtocol bank = new ChallengeProtocol(bankKeyPair);
    ChallengeProtocol atm = new ChallengeProtocol(atmKeyPair);

    atm.receiveHello(bank.sendHello());

    JsonElement payload = new JsonPrimitive(1);
    JsonElement jsonElement = bank.receiveMessage(atm.sendMessage(payload));

    assertEquals(payload, jsonElement);

    payload = new JsonObject();
    ((JsonObject) payload).addProperty("a", "b");
    jsonElement = bank.receiveMessage(atm.sendMessage(payload));

    assertEquals(payload, jsonElement);
  }
}
