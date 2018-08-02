package de.upb.cs.bibifi.commons.impl;

import de.upb.cs.bibifi.commons.IEncryption;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class EncryptionImpl implements IEncryption {

    private static final String algoName = "AES";

    private static final String transformation = "AES/CBC/PKCS5Padding";

    private byte[] key;

    private static EncryptionImpl singleton;

    private EncryptionImpl() {
    }

    public static EncryptionImpl initialize(String key) {
        if (singleton == null)
            singleton = new EncryptionImpl();

        singleton.key = Base64.getDecoder().decode(key);
        return singleton;
    }

    public static EncryptionImpl getInstance() throws Exception {
        if (singleton == null)
            throw new Exception("Encryption is not initialized");
        return singleton;
    }

    public String encryptMessage(String message) throws IOException {
        try {
            SecretKey secKey = new SecretKeySpec(key, algoName);
            Cipher aes = Cipher.getInstance(transformation);
            aes.init(Cipher.ENCRYPT_MODE, secKey, new IvParameterSpec(key));
            byte[] encryptedArray = aes.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedArray);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException |
                InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException ex) {
            throw new IOException("Error happened with encryption the message");
        }
    }

    public String decryptMessage(String string) throws IOException {
        try {
            byte[] encryptedArray = Base64.getDecoder().decode(string);
            SecretKey secKey = new SecretKeySpec(key, algoName);
            Cipher aes = Cipher.getInstance(transformation);
            aes.init(Cipher.DECRYPT_MODE, secKey, new IvParameterSpec(key));
            byte[] decryptedBytes = aes.doFinal(encryptedArray);
            return new String(decryptedBytes);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException |
                InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException ex) {
            throw new IOException("Error happened with decryption the message");
        }
    }
}
