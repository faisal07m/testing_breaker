package de.upb.bibifi2018.kaffeeklatsch;

import com.codahale.xsalsa20poly1305.SimpleBox;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import okio.ByteString;

/**
 * Represents the card file on disk.
 *
 * The card file is encrypted and signed using the ATMs key-pair
 * to ensure an attacker cannot tamper with it.
 *
 * The read file size is limited to one MiB to avoid out-of-memory errors.
 *
 */
public class CardFile {
  private final SimpleBox atmKeyPair;
  private final File cardFile;

  public CardFile(SimpleBox atmKeyPair, String cardFile) {
    this.atmKeyPair = atmKeyPair;
    this.cardFile = new File(cardFile);
  }

  public void writePrivateKey(ByteString cardKey, String accountId) throws IOException {


    JsonObject cardFile = new JsonObject();
    cardFile.addProperty(JsonConstants.ACCOUNT_KEY, accountId);
    cardFile.addProperty(JsonConstants.CARD_PRIVATEKEY_KEY, cardKey.base64());

    ByteString payload = ByteString.encodeUtf8(cardFile.toString());
    ByteString encryptedCardFile = this.atmKeyPair.seal(payload);


    Files.write(
        this.cardFile.toPath(),
        encryptedCardFile.toByteArray(),
        StandardOpenOption.CREATE_NEW
    );
  }

  public ByteString readPrivateKey(String expectedAccountId) throws InvalidCardFileException {
    try (InputStream stream = new FileInputStream(cardFile)) {

      ByteString encryptedCardKey = Util.readFullyLimited(stream, 1024 * 1024);

      ByteString payloadBytes = this.atmKeyPair
          .open(encryptedCardKey)
          .orElseThrow(() -> new InvalidCardFileException("invalid card file"));

      JsonObject payload = new JsonParser().parse(payloadBytes.utf8()).getAsJsonObject();

      String accountId = payload.getAsJsonPrimitive(JsonConstants.ACCOUNT_KEY).getAsString();
      if (!expectedAccountId.equals(accountId)) {
        throw new InvalidCardFileException("wrong card file for account");
      }

      ByteString cardPrivKey = ByteString.decodeBase64(
          payload.getAsJsonPrimitive(JsonConstants.CARD_PRIVATEKEY_KEY).getAsString()
      );

      return cardPrivKey;

    } catch (IOException e) {
      throw new InvalidCardFileException(e);
    }
  }

  public boolean exists() {
    return this.cardFile.exists();
  }
}
