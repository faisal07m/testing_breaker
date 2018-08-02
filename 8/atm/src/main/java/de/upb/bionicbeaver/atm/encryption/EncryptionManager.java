package de.upb.bionicbeaver.atm.encryption;


import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Manages data encrytion-decryption of plain data while transmission
 */
public class EncryptionManager {

    private static final Object _lock = new Object();
    private static EncryptionManager INSTANCE;

    public static EncryptionManager getInstance() {
        if(Objects.isNull(INSTANCE)) {
            throw new IllegalStateException("The encryption manager is not initialized.");
        }
        return INSTANCE;
    }

    public static void initialize(String authFileData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(Objects.isNull(INSTANCE)) {
            synchronized (_lock) {
                if(Objects.isNull(INSTANCE)) {
                    INSTANCE = new EncryptionManager(authFileData);
                }
            }
        }
    }

    private final ClientBasedSecretKey clientKey;
    private final SecretKey secretKey;
    private final IvParameterSpec iv;

    private EncryptionManager(String authFileData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        this.clientKey = new ClientBasedSecretKey(authFileData);
        this.secretKey = new SecretKeySpec(this.clientKey.getKey(), "AES");
        this.iv = new IvParameterSpec(this.clientKey.getIv());
    }

    public byte[] encrypt(byte[] message) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encrypted = cipher.doFinal(message);
            return Base64.encodeBase64(encrypted);
        } catch (Exception e) {
            return null;
        }
        /*
        byte[] cipherText = cipherEncrypt.doFinal(message);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + this.clientKey.getIv().length + cipherText.length);
        byteBuffer.putInt(this.clientKey.getIv().length);
        byteBuffer.put(this.clientKey.getIv());
        byteBuffer.put(cipherText);
        return Base64.getEncoder().encode(byteBuffer.array());
        */
    }

    public byte[] decrypt(byte[] encrypted) {
        try {
            byte[] base64Decoded = Base64.decodeBase64(encrypted);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            return cipher.doFinal(base64Decoded);
        } catch (Exception e) {
            return null;
        }
        /*
        ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted));
        int ivLength = byteBuffer.getInt();
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);
        return cipherDecrypt.doFinal(cipherText);
        */
    }

}
