package de.upb.bionicbeaver.bank.encryption;

import de.upb.bionicbeaver.bank.exception.Error;
import de.upb.bionicbeaver.bank.exception.ServerException;
import io.vavr.control.Try;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

/**
 * Manages data encrytion-decryption of plain data while transmission for a specific client
 *
 * @see ClientBasedSecretKey
 */
public class EncryptionManager {
    private static final Object _lock = new Object();

    private static EncryptionManager INSTANCE;

    public static void init(ClientBasedSecretKey clientBasedSecretKey) {
        if(Objects.isNull(INSTANCE)) {
            synchronized (_lock) {
                if(Objects.isNull(INSTANCE)) {
                    INSTANCE = Try.of(() -> new EncryptionManager(clientBasedSecretKey))
                            .onFailure(failure -> {
                                System.exit(255);
                            })
                            .get();
                }
            }
        }
    }

    public static EncryptionManager getInstance() {
        if(Objects.isNull(INSTANCE)) {
            throw new IllegalStateException("EncryptionManager is not initialized.");
        }
        return INSTANCE;
    }

    private final ClientBasedSecretKey clientBasedSecretKey;
    private final SecretKey secretKey;
    private final IvParameterSpec iv;

    /**
     * @param clientBasedSecretKey
     */
    EncryptionManager(ClientBasedSecretKey clientBasedSecretKey) {
        this.clientBasedSecretKey = clientBasedSecretKey;
        this.secretKey = new SecretKeySpec(this.clientBasedSecretKey.getKey(), "AES");
        this.iv = new IvParameterSpec(this.clientBasedSecretKey.getIv());
    }

    public byte[] encrypt(byte[] message) {

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] encrypted = cipher.doFinal(message);
            return Base64.encodeBase64(encrypted);

            /*
            byte[] cipherText = cipherEncrypt.doFinal(message);

            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + this.clientBasedSecretKey.getIv().length + cipherText.length);
            byteBuffer.putInt(this.clientBasedSecretKey.getIv().length);
            byteBuffer.put(this.clientBasedSecretKey.getIv());
            byteBuffer.put(cipherText);
            return Base64.getEncoder().encode(byteBuffer.array());
            */
        } catch (Exception e) {
            System.out.println("protocol_error");
            throw new ServerException(Error.PROTOCOL_ERROR);
        }
    }

    public byte[] decrypt(byte[] encrypted) {

        try {
            byte[] base64Decoded = Base64.decodeBase64(encrypted);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            return cipher.doFinal(base64Decoded);
            /*
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted));
            int ivLength = byteBuffer.getInt();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            return cipherDecrypt.doFinal(cipherText);
            */
        } catch (Exception e) {
            System.out.println("protocol_error");
            throw new ServerException(Error.PROTOCOL_ERROR);
        }
    }
}
