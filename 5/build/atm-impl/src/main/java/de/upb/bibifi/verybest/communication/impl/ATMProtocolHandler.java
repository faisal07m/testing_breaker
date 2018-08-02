package de.upb.bibifi.verybest.communication.impl;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.exception.*;
import de.upb.bibifi.verybest.common.messages.BodyMessage;
import de.upb.bibifi.verybest.common.messages.ChallengeMessage;
import de.upb.bibifi.verybest.common.messages.HeaderMessage;
import de.upb.bibifi.verybest.common.messages.Operation;
import de.upb.bibifi.verybest.common.models.*;
import de.upb.bibifi.verybest.common.util.MoshiProvider;
import de.upb.bibifi.verybest.communication.interfaces.IATMProtocolHandler;
import de.upb.bibifi.verybest.communication.interfaces.IATMSocketHandler;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;

public class ATMProtocolHandler implements IATMProtocolHandler {

    private final IEncryptionSigningHandler encryptionSigningHandler;
    private final IATMSocketHandler httpHandler;
    private final Moshi moshi = MoshiProvider.provideMoshi();


    public ATMProtocolHandler(IEncryptionSigningHandler encryptionSigningHandler, IATMSocketHandler httpHandler) {
        this.encryptionSigningHandler = encryptionSigningHandler;
        this.httpHandler = httpHandler;
    }

    @Override
    public Account performBasicAction(String userName, Operation operation, Action action, File userCard)
            throws OperationFailedException, CommunicationFailedException {
        if (action instanceof AccountCreateAction) {
            throw new IllegalArgumentException("Invalid operation type!");
        }
        byte[] userPrivateKey = readKey(userCard, userName);
        //Step 1: Create Init:
        ChallengeMessage challengeMessage = performInitStep();
        //Step 2: Create the actual message.
        BodyMessage bodyMessage = new BodyMessage(challengeMessage.getRandomNumber().add(BigInteger.ONE), action);
        HeaderMessage headerMessage = new HeaderMessage(userName, operation, challengeMessage.getRandomNumber().add(BigInteger.ONE));
        return performProtocolStep(headerMessage, bodyMessage, userPrivateKey, challengeMessage.getRandomNumber());
    }

    @Override
    public Account performAccountCreateAction(AccountCreateActionTemplate toPerform, File emptyCardFile)
            throws OperationFailedException, CommunicationFailedException {
        //Step 1: Create Init:
        ChallengeMessage challengeMessage = performInitStep();
        KeyPair keyPair;
        try {
            keyPair = encryptionSigningHandler.createUserKeyPair();
        } catch (CryptoInitException e) {
            e.printStackTrace();
            throw new OperationFailedException(e);
        }
        writeUserCard(emptyCardFile, keyPair.getPrivate(), toPerform.name());
        AccountCreateAction action = new AccountCreateAction(keyPair.getPublic().getEncoded(), toPerform.name(), toPerform.initialBalance());
        //Step 2: Create the actual message.
        HeaderMessage headerMessage = new HeaderMessage(action.name(), Operation.CREATE, challengeMessage.getRandomNumber().add(BigInteger.ONE), action.publicKey(),
                action.initialBalance());
        return performCreateStep(headerMessage, challengeMessage.getRandomNumber());
    }

    private ChallengeMessage performInitStep() throws OperationFailedException, CommunicationFailedException {

        try {
            byte[] receivedBytes = httpHandler.sendInitRequest();
            return receiveInitAnswer(receivedBytes);
        } catch (IOException e) {
            //Since the server is not available
            throw new CommunicationFailedException(e);
        }

    }

    private ChallengeMessage receiveInitAnswer(byte[] receivedBytes) throws OperationFailedException, CommunicationFailedException {
        try {
            byte[] decryptedBytes = encryptionSigningHandler.decrypt(receivedBytes);

            JsonAdapter<ChallengeMessage> bodyMessageJsonAdapter = moshi.adapter(ChallengeMessage.class);
            String json = new String(decryptedBytes, StandardCharsets.UTF_8);
            return bodyMessageJsonAdapter.fromJson(json);
        } catch (CryptoInitException e) {
            //Since the crypto infrastructure is not available, this is a unrecoverable error
            throw new OperationFailedException(e);
        } catch (CryptoOperationFailedException | IOException e) {
            //The message is not invalid, thus communication error
            throw new CommunicationFailedException(e);
        } catch (ProtocolException e){
            System.err.println("ATM protocol error detected.");
            throw new CommunicationFailedException(e);
        }
    }

    private Account performCreateStep(HeaderMessage headerMessage, BigInteger sessionKey) throws OperationFailedException, CommunicationFailedException {
        try {
            JsonAdapter<HeaderMessage> headerMessageJsonAdapter = moshi.adapter(HeaderMessage.class);
            String headerJson = headerMessageJsonAdapter.toJson(headerMessage);
            byte[] headerBytes = headerJson.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedHeaderBytes = encryptionSigningHandler.encrypt(headerBytes);

            byte[] receivedBytes = httpHandler.sendActionRequest(encryptedHeaderBytes, new byte[]{}, new byte[]{});

            return receiveProtocolAnswer(sessionKey, receivedBytes);
        } catch (CryptoInitException e) {
            //Since the crypto infrastructure is not available, this is a unrecoverable error
            throw new OperationFailedException(e);
        } catch (CryptoOperationFailedException | IOException e) {
            //The message is not invalid, thus communication error
            throw new CommunicationFailedException(e);
        }
    }

    /**
     * Performs the second protocol step as described in the wiki.
     *
     * @param headerMessage  for the request
     * @param bodyMessage    needs to be signed
     * @param userPrivateKey for signing
     * @return the updated account
     * @throws CommunicationFailedException if communication fails and a 63 needs to be thrown
     * @throws OperationFailedException     if the operation was declined by the client and a 255 needs to be thrown
     */
    private Account performProtocolStep(HeaderMessage headerMessage, BodyMessage bodyMessage, byte[] userPrivateKey, BigInteger sessionKey) throws CommunicationFailedException, OperationFailedException {
        JsonAdapter<BodyMessage> bodyMessageJsonAdapter = moshi.adapter(BodyMessage.class);
        String bodyJson = bodyMessageJsonAdapter.toJson(bodyMessage);
        byte[] bodyBytes = bodyJson.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] encryptedBodyBytes = encryptionSigningHandler.encrypt(bodyBytes);
            byte[] bodySignature = encryptionSigningHandler.sign(userPrivateKey, encryptedBodyBytes);
            JsonAdapter<HeaderMessage> headerMessageJsonAdapter = moshi.adapter(HeaderMessage.class);
            String headerJson = headerMessageJsonAdapter.toJson(headerMessage);
            byte[] headerBytes = headerJson.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedHeaderBytes = encryptionSigningHandler.encrypt(headerBytes);
            byte[] receivedBytes = httpHandler.sendActionRequest(encryptedHeaderBytes, bodySignature, encryptedBodyBytes);
            return receiveProtocolAnswer(sessionKey, receivedBytes);
        } catch (CryptoInitException e) {
            //Since the crypto infrastructure is not available, this is a unrecoverable error
            throw new OperationFailedException(e);
        } catch (CryptoOperationFailedException | IOException e) {
            //The message is not invalid, thus communication error
            throw new CommunicationFailedException(e);
        }
    }


    /**
     * Performs the final protocol step:
     * Decrypts the AccountAndSessionKey, verifies the session.key (the session_key is for shadowing such that two get_requests for the same bank have different responses)
     *
     * @param sessionKey    used
     * @param receivedBytes from the server
     * @return the updated account
     * @throws CommunicationFailedException if communication fails and a 63 needs to be thrown
     * @throws OperationFailedException     if the operation was declined by the client and a 255 needs to be thrown
     */
    private Account receiveProtocolAnswer(BigInteger sessionKey, byte[] receivedBytes) throws CommunicationFailedException, OperationFailedException {
        try {
            byte[] decryptedBytes = encryptionSigningHandler.decrypt(receivedBytes);
            JsonAdapter<AccountAndSession> accountJsonAdapter = moshi.adapter(AccountAndSession.class);
            AccountAndSession accountAndSessionKey = accountJsonAdapter.fromJson(new String(decryptedBytes, StandardCharsets.UTF_8));
            if (accountAndSessionKey == null) {
                System.err.println("The Account and Message object is null, this is invalid!");
                throw new CommunicationFailedException();
            }
            if (!sessionKey.add(BigInteger.valueOf(2)).equals(accountAndSessionKey.getSession())) {
                throw new CommunicationFailedException();
            }
            if (accountAndSessionKey.getErrorCode() != Constants.OK_STATUS_CODE
                    || accountAndSessionKey.getAccount() == null) {
                System.err.println("The server does not answer the request properly, the reason is "
                        + accountAndSessionKey.getErrorCode());
                throw new OperationFailedException();
            }

            return accountAndSessionKey.getAccount();
        } catch (CryptoInitException e) {
            //Since the crypto infrastructure is not available, this is a unrecoverable error
            throw new OperationFailedException(e);
        } catch (CryptoOperationFailedException | IOException e) {
            //The message is not invalid, thus communication error
            throw new CommunicationFailedException(e);
        } catch (ProtocolException e){
            System.err.println("ATM protocol error detected.");
            throw new CommunicationFailedException(e);
        }
    }


    private void writeUserCard(File file, PrivateKey userPrivateKey, String userName) throws CommunicationFailedException {
        try (Sink fileSink = Okio.sink(file);
             BufferedSink bufferedSink = Okio.buffer(fileSink)) {
            byte [] userBytes = userName.getBytes(StandardCharsets.UTF_8);
            //first the length
            bufferedSink.write(new byte[]{(byte)userBytes.length});
            //then the name
            bufferedSink.write(userBytes);
            //the key
            bufferedSink.write(userPrivateKey.getEncoded());
        } catch (IOException e) {
            throw new CommunicationFailedException(e);
        }
    }

    private byte[] readKey(File file, String userName) throws CommunicationFailedException, OperationFailedException {
        try (final BufferedSource source = Okio.buffer(Okio.source(file))) {
            // 1 byte can store lengths up to 255
            int nameLength = (int) source.readByte();
            if (nameLength <= 0 || nameLength > 122){
                //Non-compliant name length (altered user card!)
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
            byte [] nameBytes = new byte[nameLength];
            int readBytes = source.read(nameBytes);
            if (readBytes != nameLength){
                //to few bytes
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
            String userCardName = new String(nameBytes, StandardCharsets.UTF_8);
            if (!userCardName.equals(userName)){
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
            return source.readByteArray();
        } catch (final IOException exception) {
            throw new CommunicationFailedException(exception);
        }
    }
}
