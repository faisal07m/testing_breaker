package de.upb.bionicbeaver.atm.encryption;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.codec.binary.Base64;

/**
 *
 */
@Getter(value = AccessLevel.PACKAGE)
@ToString(of = "clientId")
public class ClientBasedSecretKey {

    public static final String SEPARATOR = "::";

    private final byte[] key;
    private final byte[] iv;

    public ClientBasedSecretKey(String authFileContent) {
        String[] splitted = authFileContent.split(SEPARATOR);

        key = Base64.decodeBase64(splitted[0].getBytes());
        iv = Base64.decodeBase64(splitted[1].getBytes());
    }
}
