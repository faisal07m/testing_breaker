package de.upb.bibifi.verybest.bank.communication.interfaces;

import de.upb.bibifi.verybest.bank.worker.Worker;
import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.ProtocolException;

public interface IBankProtocolHandler {

    int CHALLENGE_LENGTH = 127;

    /**
     * @param encryptionSigningHandler the util class to use
     * @param worker                   the worker
     */
    void init(IEncryptionSigningHandler encryptionSigningHandler, Worker worker);

    /**
     * Generates a secure random number of previously specified length
     *
     * @return the decrypted challenge
     * @throws CommunicationFailedException in case anything went wrong inside the crypto layer and no challenge can be created
     */
    byte[] performInitRequest() throws CommunicationFailedException;

    /**
     * @param header        message of the request
     * @param bodySignature of the send body to verify that the body is not modified
     * @param body          of the actual message
     * @return an encrypted response
     * @throws CommunicationFailedException in case the message is invalid in any sense and a "63 " needs to be returned at ATM Side
     * @throws ProtocolException if the message was altered. In this case "protocol_error" has to be printed.
     */
    byte[] performRequest(byte[] header, byte[] bodySignature, byte[] body) throws CommunicationFailedException, ProtocolException;

}
