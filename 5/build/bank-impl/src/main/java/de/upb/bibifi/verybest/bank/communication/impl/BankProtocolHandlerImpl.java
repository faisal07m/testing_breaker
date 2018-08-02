package de.upb.bibifi.verybest.bank.communication.impl;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import de.upb.bibifi.verybest.bank.communication.interfaces.IBankProtocolHandler;
import de.upb.bibifi.verybest.bank.worker.Worker;
import de.upb.bibifi.verybest.bank.worker.WorkerResult;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.CryptoInitException;
import de.upb.bibifi.verybest.common.exception.CryptoOperationFailedException;
import de.upb.bibifi.verybest.common.exception.ProtocolException;
import de.upb.bibifi.verybest.common.messages.BodyMessage;
import de.upb.bibifi.verybest.common.messages.ChallengeMessage;
import de.upb.bibifi.verybest.common.messages.HeaderMessage;
import de.upb.bibifi.verybest.common.messages.Operation;
import de.upb.bibifi.verybest.common.models.Account;
import de.upb.bibifi.verybest.common.models.AccountAndSession;
import de.upb.bibifi.verybest.common.models.AccountCreateAction;
import de.upb.bibifi.verybest.common.models.Action;
import de.upb.bibifi.verybest.common.util.MoshiProvider;
import de.upb.bibifi.verybest.common.util.RandomProvider;
import io.reactivex.Single;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;


public class BankProtocolHandlerImpl implements IBankProtocolHandler {

    @SuppressWarnings("NullAway")
    private IEncryptionSigningHandler helper;
    @SuppressWarnings("NullAway")
    private Worker worker;
    @SuppressWarnings("NullAway")
    private Random secureRandom;
    @SuppressWarnings("NullAway")
    private BigInteger session;

    private final Moshi moshi = MoshiProvider.provideMoshi();

    @Override
    public void init(IEncryptionSigningHandler encryptionSigningHandler, Worker worker) {
        this.helper = encryptionSigningHandler;
        this.worker = worker;
        this.secureRandom = RandomProvider.provideSecure();
    }

    @Override
    public byte[] performInitRequest() throws CommunicationFailedException {
        byte[] challenge = new byte[CHALLENGE_LENGTH];
        secureRandom.nextBytes(challenge);
        session = new BigInteger(challenge);
        //Create a JSON Object and parse it to bytes
        JsonAdapter<ChallengeMessage> challengeMessageJsonAdapter = moshi.adapter(ChallengeMessage.class);
        String challengeString = challengeMessageJsonAdapter.toJson(new ChallengeMessage(session));
        byte[] challengeBytes = challengeString.getBytes(StandardCharsets.UTF_8);
        //Return the encrypted Challenge
        try {
            return helper.encrypt(challengeBytes);
        } catch (CryptoInitException | CryptoOperationFailedException e) {
            //In this case, the challenge for the ATM cannot be generated. This is as specified a "SERVER_ERROR"
            e.printStackTrace();
            throw new CommunicationFailedException(e);
        }

    }

    @Override
    public byte[] performRequest(byte[] header, byte[] bodySignature, byte[] body) throws CommunicationFailedException, ProtocolException {

        //Encrypt
        byte[] plainHeader;
        try {
            plainHeader = helper.decrypt(header);
        } catch (CryptoOperationFailedException | CryptoInitException e) {
            //Since the upper layer do not care why the operation is failed, this is collected to a single error message
            throw new CommunicationFailedException(e);
        } catch (ProtocolException e) {
            System.err.println("Protocol error detected! No rewind possible as we cannot guarantee the integrity of the sent message!");
            throw e;
        }

        //Parse the header
        HeaderMessage headerMessage = getHeaderMessageFromJson(plainHeader);

        if (headerMessage == null) {
            System.err.println("The header message is null, this is a protocol exception! No rewind possible as we cannot guarantee the integrity of the message.");
            throw new ProtocolException(new IllegalArgumentException("Null header"));
        }


        //Verify that the header Message contains a valid nonce since nonce-1 is stored and not timed out
        if (!headerMessage.getRandomNumber().subtract(BigInteger.ONE).equals(session)) {
            throw new ProtocolException(new IllegalArgumentException("Invalid session"));
        }
        if (headerMessage.getOperation().equals(Operation.CREATE)) {
            return parseCreateMessage(headerMessage);
        } else {
            //Firstly, verify that body signature is valid for the given body to avoid parsing manipulated information
            if (body.length == 0 || bodySignature.length == 0) {
                System.err.println("The given body or body signature message was null and was intended to be not null. This is a protocol error. Rewinding...");
                throw new ProtocolException(new IllegalArgumentException("Null field detected."));
            }

            byte[] userPublicKey = getUserPublicKeyForName(headerMessage.getName());
            if (userPublicKey == null) {
                return encryptAccountAndSession(
                        new AccountAndSession(null, headerMessage.getRandomNumber().add(BigInteger.ONE), Constants.USER_NOT_FOUND));
            }

            BodyMessage bodyMessage;
            try {
                bodyMessage = getBodyMessageFromJson(bodySignature, body, userPublicKey);
            } catch (ProtocolException e) {
                System.err.println("Bank found a protocol error! Rewinding...");
                throw e;
            }
            if (bodyMessage == null) {
                System.err.println("The body message is null, this is a protocol_error. Rewinding...");
                throw new ProtocolException(new IllegalArgumentException("Null body."));
            }
            //Check if body and header belong together
            if (!areMatchingNonces(headerMessage, bodyMessage)) {
                System.err.println("The body message does not match the header message. This is a protocol_error. Rewinding...");
                throw new ProtocolException(new IllegalArgumentException("Body session doesn't match header session."));
            }
            if (headerMessage.getOperation().equals(Operation.GET)
                    || headerMessage.getOperation().equals(Operation.WITHDRAW)
                    || headerMessage.getOperation().equals(Operation.DEPOSIT)
                    ) {
                return parseActionMessage(headerMessage, bodyMessage.getAction());
            } else {
                throw new IllegalArgumentException("The given operation is not known!");
            }
        }
    }

    private BodyMessage getBodyMessageFromJson(byte[] bodySignature, byte[] body, byte[] userPublicKey) throws CommunicationFailedException, ProtocolException {
        try {
            byte[] plainBody = getEncryptedBodyIfValid(bodySignature, body, userPublicKey);
            String bodyJson = new String(plainBody, StandardCharsets.UTF_8);
            JsonAdapter<BodyMessage> bodyMessageJsonAdapter = moshi.adapter(BodyMessage.class);
            return bodyMessageJsonAdapter.fromJson(bodyJson);
        } catch (IOException e) {
            //Since the upper layer do not care why the operation is failed, this is collected to a single error message
            System.err.println("Malformed JSON");
            throw new CommunicationFailedException(e);
        } catch (ProtocolException e) {
            System.err.println("Protocol error detected!");
            throw e;
        }

    }

    private HeaderMessage getHeaderMessageFromJson(byte[] plainHeader) throws CommunicationFailedException {
        HeaderMessage headerMessage;
        try {
            String headerJson = new String(plainHeader, StandardCharsets.UTF_8);
            JsonAdapter<HeaderMessage> headerMessageJsonAdapter = moshi.adapter(HeaderMessage.class);
            headerMessage = headerMessageJsonAdapter.fromJson(headerJson);
        } catch (IOException e) {
            //Since the upper layer do not care why the operation is failed, this is collected to a single error message
            System.err.println("Malformed JSON");
            throw new CommunicationFailedException(e);
        }
        return headerMessage;
    }

    private byte[] getEncryptedBodyIfValid(byte[] bodySignature, byte[] body, byte[] userPublicKey) throws CommunicationFailedException, ProtocolException {
        byte[] plainBody;

        try {
            if (helper.verify(userPublicKey, body, bodySignature)) {
                plainBody = helper.decrypt(body);
            } else {
                System.err.println("The given body was modified and is thus not encrypted");
                throw new CommunicationFailedException();
            }
        } catch (CryptoInitException | CryptoOperationFailedException e) {
            //In this case, the challenge for the ATM cannot be generated. This is as specified a "SERVER_ERROR"
            e.printStackTrace();
            throw new CommunicationFailedException(e);
        } catch (ProtocolException e) {
            System.err.println("Protocol error detected!");
            throw e;
        }

        return plainBody;
    }


    private byte[] parseCreateMessage(HeaderMessage headerMessage) throws CommunicationFailedException {
        AccountCreateAction create = new AccountCreateAction(headerMessage.getUserPK(), headerMessage.getName(), headerMessage.getAmount());
        return parseActionMessage(headerMessage, create);
    }

    private byte[] parseActionMessage(HeaderMessage headerMessage, Action action) throws CommunicationFailedException {

        WorkerResult res = worker.enqueue(action).blockingGet();

        if (res.kind().equals(WorkerResult.Kind.Failure)) {
            return encryptAccountAndSession(new AccountAndSession(null, headerMessage.getRandomNumber().add(BigInteger.ONE), Constants.BANK_REFUSED_TRANSACTION));
        } else {
            Account account = res.success().newAccountStatus();
            AccountAndSession returnObj = new AccountAndSession(account, headerMessage.getRandomNumber().add(BigInteger.ONE), Constants.OK_STATUS_CODE);
            //Create a JSON Object and parse it to bytes
            return encryptAccountAndSession(returnObj);
        }
    }

    private byte[] encryptAccountAndSession(AccountAndSession returnObj) throws CommunicationFailedException {
        JsonAdapter<AccountAndSession> accountJsonAdapter = moshi.adapter(AccountAndSession.class);
        String accountString = accountJsonAdapter.toJson(returnObj);
        byte[] returnBytes = accountString.getBytes(StandardCharsets.UTF_8);
        //Return the encrypted answer
        try {
            return helper.encrypt(returnBytes);
        } catch (CryptoInitException | CryptoOperationFailedException e) {
            //In this case, the challenge for the ATM cannot be generated. This is as specified a "SERVER_ERROR"
            e.printStackTrace();
            throw new CommunicationFailedException(e);
        }
    }

    @Nullable
    private byte[] getUserPublicKeyForName(String name) {
        Single<Optional<byte[]>> key = worker.publicKeyForAccountName(name);
        final Optional<byte[]> keyBytes = key.blockingGet();
        return keyBytes.orElse(null);
    }

    private boolean areMatchingNonces(HeaderMessage headerMessage, BodyMessage bodyMessage) {
        return headerMessage.getRandomNumber().equals(bodyMessage.getRandomNumber());
    }


}
