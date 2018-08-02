package de.upb.bibifi2018.kaffeeklatsch.protocol;

import java.security.SecureRandom;
import okio.ByteString;

/**
 * Generate challenges for the {@link ChallengeProtocol}s.
 *
 * {@link SecureRandom#getInstanceStrong()} will try to read from
 * {@code /dev/random} and will block forever if the device is not
 * properly seeded. {@link SecureRandom} is good enough, even our
 * NaCl implementation is using it in {@link com.codahale.xsalsa20poly1305.Keys#generateSecretKey()}
 */
public class ChallengeGenerator {

  /**
   * {@link SecureRandom} is thread-safe.
   */
  private static final SecureRandom random = new SecureRandom();

  public static ByteString generateChallenge() {
    byte[] buffer = new byte[32];
    random.nextBytes(buffer);
    return ByteString.of(buffer);
  }
}

