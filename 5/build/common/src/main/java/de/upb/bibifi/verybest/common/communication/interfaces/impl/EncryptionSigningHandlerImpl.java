package de.upb.bibifi.verybest.common.communication.interfaces.impl;

import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.exception.CryptoInitException;
import de.upb.bibifi.verybest.common.exception.CryptoOperationFailedException;
import de.upb.bibifi.verybest.common.exception.ProtocolException;
import okio.BufferedSource;
import okio.Okio;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionSigningHandlerImpl implements IEncryptionSigningHandler {


    private final byte[] authFile;

    private final byte[] nonce;


    public EncryptionSigningHandlerImpl(final File authFile) throws CryptoInitException {
        try (final BufferedSource source = Okio.buffer(Okio.source(authFile))) {
            byte[] keyAndNonce = source.readByteArray();
            this.nonce = new byte[16];
            this.authFile = new byte[16];
            System.arraycopy(keyAndNonce, 0, nonce, 0, 16);
            System.arraycopy(keyAndNonce, 16, this.authFile, 0, 16);
        } catch (final IOException exception) {
            System.err.println("Could not read authFile");
            throw new CryptoInitException(exception);
        } catch (final ArrayIndexOutOfBoundsException exception) {
            System.err.println("The authfile's content is invalid");
            throw new CryptoInitException(exception);
        }

        //Check if the key is invalid, in this case the bank needs to exit with 255
        try {
            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            SecretKeySpec keySpec = new SecretKeySpec(this.authFile, KEY_MODE);
            aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(nonce));
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Unable to initialize the cryptographic cipher");
            throw new CryptoInitException(e);
        }

    }

    @Override
    public byte[] encrypt(byte[] toEncrypt) throws CryptoInitException, CryptoOperationFailedException {
        try {
            //Init the algorithm
            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            SecretKeySpec keySpec = new SecretKeySpec(authFile, KEY_MODE);
            aesCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(nonce));

            //encrypt the text
            return aesCipher.doFinal(Base64.getEncoder().encode(toEncrypt));
        } catch (BadPaddingException e) {
            System.err.println("The used padding algorithm was not appropriated for the used scheme");
            throw new CryptoOperationFailedException(e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("The used block size was illegal");
            throw new CryptoOperationFailedException(e);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | NoSuchPaddingException | InvalidKeyException e) {
            //This errors are thrown in the constructor / init method, so just collect and thrown a CryptoInitException
            throw new CryptoInitException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] toDecrypt) throws CryptoOperationFailedException, CryptoInitException, ProtocolException {
        try {
            //Init the algorithm
            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            SecretKeySpec keySpec = new SecretKeySpec(authFile, KEY_MODE);
            aesCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(nonce));

            //decrypt th text
            try {
                return Base64.getDecoder().decode(aesCipher.doFinal(toDecrypt));
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to decrypt: No base64 based result (probably altered message)");
                throw new ProtocolException(e);
            }

        } catch (BadPaddingException e) {
            System.err.println("The used padding algorithm was not appropriated for the used scheme");
            throw new ProtocolException(e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("The used block size was illegal");
            throw new CryptoOperationFailedException(e);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException
                | NoSuchPaddingException | InvalidKeyException e) {
            //This errors are thrown in the constructor / init method, so just collect and thrown a CryptoInitException
            throw new CryptoInitException(e);
        }
    }

    @Override
    public boolean verify(byte[] userPublicKey, byte[] message, byte[] signature) throws CryptoOperationFailedException, CryptoInitException {
        try {
            //Init signature
            Signature dsa = Signature.getInstance(SIGN_ALGORITHM);
            X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(userPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pk = keyFactory.generatePublic(bobPubKeySpec);
            dsa.initVerify(pk);

            //Verify signature
            dsa.update(message);
            return dsa.verify(signature);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("The SHA256withRSA algorithm was not available");
            throw new CryptoInitException(e);
        } catch (SignatureException e) {
            System.err.println("The SHA256withRSA algorithm was not initialized properly!");
            throw new CryptoInitException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoOperationFailedException(e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoOperationFailedException(e);
        }
    }

    @Override
    public byte[] sign(byte[] userCard, byte[] message) throws CryptoInitException, CryptoOperationFailedException {
        try {
            //Init signature
            Signature rsa = Signature.getInstance(SIGN_ALGORITHM);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(userCard);
            PrivateKey pk = keyFactory.generatePrivate(privateKeySpec);
            rsa.initSign(pk);

            //Add the data to sign
            rsa.update(message);

            //Create the signature
            return rsa.sign();

        } catch (NoSuchAlgorithmException e) {
            System.err.println("The SHA256withRSA algorithm was not available");
            throw new CryptoInitException(e);
        } catch (SignatureException e) {
            System.err.println("The SHA256withRSA algorithm was not initialized properly!");
            throw new CryptoInitException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoOperationFailedException(e);
        } catch (InvalidKeySpecException e) {
            throw new CryptoOperationFailedException(e);
        }
    }

    @Override
    public KeyPair createUserKeyPair() throws CryptoInitException {

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(DSA_KEYSIZE);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("The Key Generation for the RSA Algorithm failed, since there is no such algorithm.");
            throw new CryptoInitException(e);
        }

    }
}
