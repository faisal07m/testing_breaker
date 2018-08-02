package de.upb.bibifi2018.kaffeeklatsch;

import com.codahale.xsalsa20poly1305.SimpleBox;
import okio.ByteString;

public class AuthKeys {
  private final ByteString atmPrivateKey;
  private final ByteString bankPublicKey;

  public AuthKeys(ByteString atmPrivateKey, ByteString bankPublicKey) {
    this.atmPrivateKey = atmPrivateKey;
    this.bankPublicKey = bankPublicKey;
  }

  public ByteString getAtmPrivateKey() {
    return atmPrivateKey;
  }

  public ByteString getBankPublicKey() {
    return bankPublicKey;
  }

  public SimpleBox asSimpleBox() {
    return new SimpleBox(this.bankPublicKey, this.atmPrivateKey);
  }
}
