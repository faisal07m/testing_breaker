package de.upb.bionicbeaver.bank.encryption;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;

import java.security.SecureRandom;

/**
 *
 */
@Getter(value = AccessLevel.PACKAGE)
public class ClientBasedSecretKey {
    public static final int KEY_SIZE_BITS = 128;
    public static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8;

    public static final int IV_SIZE_BITS = 128;
    public static final int IV_SIZE_BYTES = IV_SIZE_BITS / 8;

    public static final String SEPARATOR = "::";

    private final byte[] key = new byte[KEY_SIZE_BYTES];
    private final byte[] iv = new byte[IV_SIZE_BYTES];

    public ClientBasedSecretKey() {
        //Random secureRandom = new Random();
        SecureRandom secureRandom = Try.of(() -> SecureRandom.getInstance("SHA1PRNG")).onFailure(failure -> System.exit(255)).get();
        //this.key = new byte[] {109,112,28,(byte)195,(byte)236,(byte)189,114,(byte)220,94,(byte)134,122,10,60,87,31,(byte)153};
        secureRandom.nextBytes(this.key);
        //this.iv = new byte[] {25,(byte)169,59,106,119,(byte)253,(byte)233,111,74,(byte)164,(byte)188,(byte)196, 109,112,28,(byte)195};
        secureRandom.nextBytes(this.iv);
    }

    public String getCombinedKey() {
        String base64Key = Base64.encodeBase64String(this.key);
        String base64IV = Base64.encodeBase64String(this.iv);

        return new StringBuilder()
                .append(base64Key)
                .append(SEPARATOR)
                .append(base64IV)
                .toString();
    }
}
