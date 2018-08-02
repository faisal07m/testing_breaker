package de.upb.bibifi.verybest.communication.interfaces;

import java.io.IOException;
import java.net.Inet4Address;

public interface IATMSocketHandler {

    void init(Inet4Address bankHost, int bankPort) throws IOException;

    /**
     * Sends the init request to the server and returns the encrypted session key.
     *
     * @return
     * @throws IOException           Communication to server failed
     * @throws IllegalStateException Response did not indicate success
     */
    byte[] sendInitRequest() throws IOException;

    /**
     * Sends an action request to the bank and returns the encrypted account + session_key
     *
     * @throws IOException           Communication to server failed
     * @throws IllegalStateException Response did not indicate success
     */
    byte[] sendActionRequest(byte[] header, byte[] bodySignature, byte[] body) throws IOException;

}
