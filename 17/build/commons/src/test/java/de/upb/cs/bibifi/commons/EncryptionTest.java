package de.upb.cs.bibifi.commons;

import de.upb.cs.bibifi.commons.impl.EncryptionImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncryptionTest {

    @Test
    public void encryptionTest() throws Exception {
        String helloMessage = "Hello encryption";
        String key = "PrvQH+6bvZPJrqR02ntOFw";
        IEncryption encryption = EncryptionImpl.initialize(key);

        String encryptedString = encryption.encryptMessage(helloMessage);
        String decryptedString = encryption.decryptMessage(encryptedString);

        assertEquals(helloMessage, decryptedString);
    }
}
