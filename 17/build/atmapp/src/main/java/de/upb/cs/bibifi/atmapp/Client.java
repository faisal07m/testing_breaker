package de.upb.cs.bibifi.atmapp;

import com.google.gson.Gson;
import de.upb.cs.bibifi.atmapp.util.CommandLineHandler;
import de.upb.cs.bibifi.commons.IEncryption;
import de.upb.cs.bibifi.commons.constants.AppConstants;
import de.upb.cs.bibifi.commons.dto.CreationResponse;
import de.upb.cs.bibifi.commons.dto.Response;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;
import de.upb.cs.bibifi.commons.enums.RequestType;
import de.upb.cs.bibifi.commons.impl.EncryptionImpl;
import de.upb.cs.bibifi.commons.impl.Utilities;
import de.upb.cs.bibifi.commons.validator.InputPatternChecker;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

public class Client implements IClient {

    private final String cardFileName;
    private final String ip;
    private final Integer port;

    private Client(String cardFileName, String ip, Integer port) {
        this.cardFileName = cardFileName;
        this.ip = ip;
        this.port = port;
    }

    private void clientRequest(TransmissionPacket request) throws Exception {

        String jsonRequest = Utilities.serializer(request);

        try {
            Socket sock = new Socket(ip, port);
            sock.setSoTimeout(AppConstants.SOCKET_TIMEOUT);

            OutputStream outputStream = sock.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);
            InputStream inputStream = sock.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            IEncryption encryption = EncryptionImpl.getInstance();
            String encryptString = encryption.encryptMessage(jsonRequest);
            printWriter.println(encryptString);
            printWriter.flush();
            //Receive Response
            String receivedMessage;
            if ((receivedMessage = br.readLine()) != null) {
                receivedMessage = encryption.decryptMessage(receivedMessage);
                Gson gson = new Gson();
                Response responseObject;
                if (request.getRequestType() == RequestType.CREATE) {
                    responseObject = gson.fromJson(receivedMessage, CreationResponse.class);
                } else {
                    responseObject = gson.fromJson(receivedMessage, Response.class);
                }
                if (responseObject.getCode() == 255 || responseObject.getCode() == 67) {
                    System.exit(255);
                }
                if (responseObject.getCode() == 0) {
                    if (request.getRequestType() == RequestType.CREATE) {
                        CreationResponse responseCreationObject = (CreationResponse) responseObject;
                        savePin(responseCreationObject.getPin());
                    }
                    System.out.println(responseObject.getMessage());
                    System.out.flush();
                    printWriter.flush();
                    sock.close();
                }
            } else{
                System.exit(63);
            }
        } catch (IllegalBlockingModeException | IllegalArgumentException | IOException | InterruptedException ex) {
            System.err.println(63);
            System.err.println(ex.getMessage());
            System.exit(63);
        }
    }

    private void savePin(String pin) throws Exception {
        File file = new File(cardFileName);
        if (file.exists()) {
            System.err.println(255);
            fail();
        }
        FileUtils.writeStringToFile(file, EncryptionImpl.getInstance().encryptMessage(pin), "UTF-8");
    }

    public static void main(String[] args) {
        if (!InputPatternChecker.check(args)) {
            System.err.println(255);
            System.exit(255);
        }

        CommandLineHandler commandLineHandler = new CommandLineHandler(args);
        TransmissionPacket packet = commandLineHandler.processCommandLineArguments().getPacket();
        Client client = new Client(commandLineHandler.getCardFileName(), commandLineHandler.getIp(), commandLineHandler.getPort());
        try {
            client.clientRequest(packet);
        } catch (Exception e) {
            System.err.println(255);
            System.exit(255);
        }
    }

    private void fail() {
        System.exit(255);
    }

}