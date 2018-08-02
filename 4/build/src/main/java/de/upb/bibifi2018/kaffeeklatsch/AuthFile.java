package de.upb.bibifi2018.kaffeeklatsch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import okio.ByteString;

/**
 * This is the auth file containing the pre-shared secret of bank and atm.
 * <br><br>
 *
 * <p>
 * Still, it is checked that we only read at most 1MiB before exiting with an error
 * to avoid out-of-memory situations.
 * </p>
 */
public class AuthFile {
  private static final String ATM_PRIVATE_KEY = "atm_private_key";
  private static final String BANK_PUBLIC_KEY = "bank_public_key";
  private final File authFile;

  public AuthFile(String authFile) {
    this.authFile = new File(authFile);
  }

  public void writeKeys(ByteString atmPrivateKey, ByteString bankPublicKey) throws IOException {

    JsonObject envelope = new JsonObject();
    envelope.addProperty(ATM_PRIVATE_KEY, atmPrivateKey.base64());
    envelope.addProperty(BANK_PUBLIC_KEY, bankPublicKey.base64());

    Files.write(
        this.authFile.toPath(),
        envelope.toString().getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE_NEW
    );

  }

  public AuthKeys readPrivateKey() throws IOException {
    try (InputStream stream = new FileInputStream(authFile)) {

      String jsonString = Util.readFullyLimited(stream, 1024 * 1024).utf8();

      JsonObject envelope = new JsonParser().parse(jsonString).getAsJsonObject();

      ByteString atmPrivateKey = ByteString.decodeBase64(
          envelope.getAsJsonPrimitive(ATM_PRIVATE_KEY).getAsString()
      );

      ByteString bankPublicKey = ByteString.decodeBase64(
          envelope.getAsJsonPrimitive(BANK_PUBLIC_KEY).getAsString()
      );

      return new AuthKeys(atmPrivateKey, bankPublicKey);
    }
  }

}
