package de.upb.bibifi.verybest.common.communication.interfaces;

import de.upb.bibifi.verybest.common.exception.CryptoInitException;
import de.upb.bibifi.verybest.common.exception.CryptoOperationFailedException;
import de.upb.bibifi.verybest.common.exception.ProtocolException;

import java.security.KeyPair;

public interface IEncryptionSigningHandler {

    String SIGN_ALGORITHM = "SHA256withRSA";
    String AES_MODE = "AES/CBC/PKCS5Padding";
    String KEY_MODE = "AES";
    int DSA_KEYSIZE = 1024;


    /**
     * Encrypt a message
     *
     * @param toEncrypt the plain-text
     * @return the cipher-text
     * @throws CryptoInitException            if the algorithm is not initialized correctly (cannot happen)
     * @throws CryptoOperationFailedException if the encryption input failed
     */
    byte[] encrypt(byte[] toEncrypt) throws CryptoInitException, CryptoOperationFailedException;

    /**
     * Decrypt a message
     *
     * @param toDecrypt the cipher-text
     * @return the plain-text
     * @throws CryptoInitException            if the algorithm is not initialized correctly (cannot happen)
     * @throws CryptoOperationFailedException if the encryption input failed
     */
    byte[] decrypt(byte[] toDecrypt) throws CryptoOperationFailedException, CryptoInitException, ProtocolException;

    /**
     * verify a given signature for a message
     *
     * @param userPublicKey used to check the signature of the message
     * @param message       to verify
     * @param signature     to check against
     * @return result of the check
     * @throws CryptoOperationFailedException in case that the key is not a valid SHA256withDSA key
     * @throws CryptoInitException            if the algorithm is not initialized properly
     */
    boolean verify(byte[] userPublicKey, byte[] message, byte[] signature) throws CryptoOperationFailedException, CryptoInitException;

    /**
     * @return a valid key pair for signing and verification of signatures
     */
    KeyPair createUserKeyPair() throws CryptoInitException;


    /**
     * sign a message
     *
     * @param userCard used to create the signature of the message
     * @param message  to sign
     * @return signature of the message
     * @throws CryptoOperationFailedException in case that the key is not a valid SHA256withDSA key
     * @throws CryptoInitException            if the algorithm is not initialized properly
     */
    byte[] sign(byte[] userCard, byte[] message) throws CryptoInitException, CryptoOperationFailedException;
}
