package de.upb.bibifi2018.kaffeeklatsch.protocol;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import okio.ByteString;

/**
 * This is a base class implementing the mechanics of a challenge-response
 * protocol to mitigate replay attacks.
 * It generates signed and encrypted messages containing a payload besides the challenge.
 *
 * <br><br>
 * Once Side has to initiate the conversation with a challenge-Hello,
 * which the other side will echo from that point in every message it sends.
 *
 * <br><br>
 * The not-initiating side will send its challenge in the first message it sends,
 * and the other side will echo it in the same way in all following messages.
 *
 */
public class ChallengeProtocol {
  private final SimpleBox keyPair;
  private final ByteString localChallenge;
  private ByteString remoteChallenge;
  private boolean localChallengeSent = false;
  private boolean remoteChallengeReceived = false;

  public ChallengeProtocol(SimpleBox keyPair) {
    this.keyPair = keyPair;
    this.localChallenge = ChallengeGenerator.generateChallenge();
  }

  protected ByteString sendHello() {
    if (this.remoteChallengeReceived || this.localChallengeSent) {
      throw new IllegalStateException("already initialized");
    }
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(JsonConstants.CHALLENGE_REQUEST, this.localChallenge.base64());
    ByteString message = EncryptDecrypt.encryptSignJson(this.keyPair, jsonObject);

    this.localChallengeSent = true;
    return message;
  }

  protected void receiveHello(ByteString hello) throws ProtocolFailureException {
    if (this.remoteChallengeReceived || this.localChallengeSent) {
      throw new IllegalStateException("already initialized");
    }
    JsonObject envelope = EncryptDecrypt.verifyDecrypt(this.keyPair, hello);

    ByteString challengeRequest = ByteString.decodeBase64(
        envelope.getAsJsonPrimitive(JsonConstants.CHALLENGE_REQUEST).getAsString()
    );
    this.remoteChallenge = challengeRequest;
    this.remoteChallengeReceived = true;
  }

  protected final ByteString sendMessage(JsonElement payload) {
    JsonObject envelope = new JsonObject();
    envelope.add(JsonConstants.PAYLOAD, payload);
    if (!this.localChallengeSent) {
      envelope.addProperty(JsonConstants.CHALLENGE_REQUEST, this.localChallenge.base64());
      this.localChallengeSent = true;
    }
    if (this.remoteChallengeReceived) {
      envelope.addProperty(JsonConstants.CHALLENGE_ECHO, this.remoteChallenge.base64());
    }
    return EncryptDecrypt.encryptSignJson(this.keyPair, envelope);
  }

  protected final JsonElement receiveMessage(ByteString message)
      throws ProtocolFailureException, ReplayAttackException {

    JsonObject envelope = EncryptDecrypt.verifyDecrypt(this.keyPair, message);

    if (this.localChallengeSent) {
      ByteString challengeEcho = ByteString.decodeBase64(
          envelope.getAsJsonPrimitive(JsonConstants.CHALLENGE_ECHO).getAsString()
      );
      if (!this.localChallenge.equals(challengeEcho)) {
        throw new ReplayAttackException("wrong challenge echo");
      }
    }

    if (!this.remoteChallengeReceived) {
      ByteString remoteChallenge = ByteString.decodeBase64(
          envelope.getAsJsonPrimitive(JsonConstants.CHALLENGE_REQUEST).getAsString()
      );
      this.remoteChallenge = remoteChallenge;
      this.remoteChallengeReceived = true;
    }

    JsonElement payload = envelope.get(JsonConstants.PAYLOAD);

    return payload;
  }

  public ByteString getLocalChallenge() {
    return localChallenge;
  }

  public ByteString getRemoteChallenge() {
    return remoteChallenge;
  }
}
