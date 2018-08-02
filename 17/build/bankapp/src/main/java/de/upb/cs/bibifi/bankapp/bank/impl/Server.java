package de.upb.cs.bibifi.bankapp.bank.impl;

import de.upb.cs.bibifi.bankapp.bank.IServer;
import de.upb.cs.bibifi.bankapp.bank.IServerProcessor;
import de.upb.cs.bibifi.commons.IEncryption;
import de.upb.cs.bibifi.commons.constants.AppConstants;
import de.upb.cs.bibifi.commons.data.AuthFile;
import de.upb.cs.bibifi.commons.dto.Response;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;
import de.upb.cs.bibifi.commons.impl.EncryptionImpl;
import de.upb.cs.bibifi.commons.impl.Utilities;
import de.upb.cs.bibifi.commons.validator.Validator;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;


public class Server implements IServer {

    private ServerSocket serverSocket;
    private IServerProcessor processor;

    private IEncryption encryption;
    private String authFile;

    public static void main(String[] args) {

        CommandLineParser commandLineParser = new DefaultParser();

        CommandLine commandLine = null;

        Options options = new Options();

        options.addOption("s", "authfile", true, "Authentication File");
        options.addOption("p", "port", true, "port");

        try {
            commandLine = commandLineParser.parse(options, args);

            Validator.applyValidators(commandLine.getOptions());

        } catch (UnrecognizedOptionException ex) {
            System.exit(255);
        } catch (ParseException e) {
            System.exit(255);
        }

        try {
            IServer server = new Server(Integer.parseInt(commandLine.getOptionValue("port", String.valueOf(AppConstants.DEFAULT_PORT_NUMBER)))
                    , commandLine.getOptionValue("authfile", AppConstants.DEFAULT_AUTH_FILE_NAME));
            server.start();
        } catch (Exception e) {
            System.exit(255);
        }
    }

    private Server(int port, String authFile) throws Exception {
        this.authFile = authFile;
        this.serverSocket = new ServerSocket(port);
        this.processor = ServerProcessor.getServerProcessor();
        Bank.getBank().startup(authFile);
        setUpShutDownHock();
        encryption = EncryptionImpl.initialize(AuthFile.getAuthFile(this.authFile).getKey());
    }

    private void setUpShutDownHock() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }


    @Override
    public void start() throws Exception {
        PrintWriter print = null;
        while (true) {
            Socket sock = null;
            try {
                //Open Socket for accepting request
                sock = serverSocket.accept();
                OutputStream out = sock.getOutputStream();
                print = new PrintWriter(out, true);

                sock.setSoTimeout(AppConstants.SOCKET_TIMEOUT);
                InputStream istream = sock.getInputStream();
                BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
                String receiveMessage, decryptMsg = null;

                //Receive msg and decrypt the message
                if ((receiveMessage = receiveRead.readLine()) != null) {
                    decryptMsg = encryption.decryptMessage(receiveMessage);
                }

                //Take decrypted msg and make pkt
                if (decryptMsg != null) {
                    String json = decryptMsg.toString();

                    TransmissionPacket requestPkt = Utilities.deserializer(json);
                    Response response = processor.executeOperation(requestPkt);

                    if (response.getCode() == 0) {
                        System.out.println(response.getMessage());
                        System.out.flush();
                    }

                    Gson gson = new Gson();
                    String resJson = gson.toJson(response);
                    String encryptResponse = encryption.encryptMessage(resJson);
                    print.println(encryptResponse);
                    print.flush();
                }
            } catch (IllegalArgumentException ex) {
                System.err.println(255);
                fail();
            } catch (SocketTimeoutException | IllegalBlockingModeException ex) {
                System.out.println("protocol_error");
                System.out.flush();
                print.flush();
                sock.close();
            }
        }
    }

    private void cleanup() throws IOException {
        FileUtils.forceDelete(new File(authFile));
    }


    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                cleanup();
            } catch (IOException e) {
                System.exit(255);
            }
        }
    }

    private void fail() {
        System.err.println(255);
    }
}