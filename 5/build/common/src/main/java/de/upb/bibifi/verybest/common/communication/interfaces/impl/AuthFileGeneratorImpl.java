package de.upb.bibifi.verybest.common.communication.interfaces.impl;

import de.upb.bibifi.verybest.common.communication.interfaces.IAuthFileGenerator;
import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.util.RandomProvider;
import okio.BufferedSink;
import okio.Okio;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AuthFileGeneratorImpl implements IAuthFileGenerator {
    @Override
    public void generateAuthFile(File emptyFile) throws IOException {
        try {
            if (emptyFile.length() != 0) {
                throw new IOException("The file is not empty but was intended to be empty");
            }
            //creating a nonce
            Random random = RandomProvider.provideSecure();
            byte [] nonce = new byte[16];
            random.nextBytes(nonce);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(IEncryptionSigningHandler.KEY_MODE);
            SecretKey aesKey = keyGenerator.generateKey();
            try (BufferedSink buffer = Okio.buffer(Okio.sink(emptyFile))) {
                buffer.write(nonce);
                buffer.write(aesKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("The algorithm " + IEncryptionSigningHandler.AES_MODE + "does not exist");
            throw new AssertionError(e);
        }
    }
}
