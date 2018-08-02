package de.upb.bionicbeaver.atm;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.upb.bionicbeaver.atm.encryption.EncryptionManager;
import de.upb.bionicbeaver.atm.model.Request;
import de.upb.bionicbeaver.atm.model.Response;
import de.upb.bionicbeaver.atm.model.TransactionType;
import de.upb.bionicbeaver.atm.util.JsonMapperProvider;
import de.upb.bionicbeaver.atm.validation.AtmCLIValidator;
import de.upb.bionicbeaver.atm.validation.ModeOfOperation;
import io.vavr.control.Try;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Main class to start atm app.
 */
public class Atm {
    private static final int END_OF_STREAM = 46;

    public static void main(String[] args) {
        replaceErrorPrintStream();

        String authFileLocation = getAuthFileLocation(args);
        // Start with input validation.
        AtmCLIValidator input = parseProgramArguments(args);
        if(Objects.isNull(input)) {
            System.exit(255);
        }
        // The last argument is the location where the auth file must be placed.
        loadAuthFileEncryptionManager(input.getAuthFile(), authFileLocation);
        // Send request to server
        Optional<Socket> socket = null;
        Request initialRequest = null;

        try {

            // Create socket connection
            socket = createSocket(input);
            if(!socket.isPresent()) {
                System.exit(63);
            }

            initialRequest = generateInitialRequest(input.getAccount(), input.getCardFile(), input.getModeOfOperation(), input.getMooValue());
            byte[] initialRequestByteArr = JsonMapperProvider.getInstance().get().writeValueAsString(initialRequest).getBytes();
            byte[] initialRequestEncrypted = EncryptionManager.getInstance().encrypt(initialRequestByteArr);
            sendDataToSocket(socket.get(), initialRequestEncrypted);

        } catch (Exception exception) {
            handleFailure(exception);
        }

        // Receive response from server
        Response response = null;
        try {

            byte[] rawResponse = readDataFromSocket(socket.get());
            if (Objects.isNull(rawResponse)) {
                socket.get().close();
                System.exit(63);
            }
            response = JsonMapperProvider.getInstance().get().readValue(
                    EncryptionManager.getInstance().decrypt(rawResponse),
                    Response.class);

            if(Objects.nonNull(response.getError())) {
                handleErrorResponse(response, socket.get());
            } else {
                handleSuccessfulResponse(response);
            }

        } catch (Exception exception) {
            handleFailure(exception);
        } finally {
            closeSocket(socket.get());
        }

        // Send ACK
        try {
            if(Objects.nonNull(response) && Objects.nonNull(initialRequest)) {
                if(initialRequest.getRequestType() != TransactionType.GET_BALANCE) {
                    Request ackRequest = new Request(initialRequest.getId(), initialRequest.getAccountName(), initialRequest.getCard(), TransactionType.ACK, BigDecimal.ZERO);
                    byte[] ackRequestByteArr = JsonMapperProvider.getInstance().get().writeValueAsBytes(ackRequest);
                    byte[] ackRequestEncrypted = EncryptionManager.getInstance().encrypt(ackRequestByteArr);
                    socket = createSocket(input);
                    if(!socket.isPresent()) {
                        System.exit(63);
                    }
                    sendDataToSocket(socket.get(), ackRequestEncrypted);
                }
            }
        } catch (Exception exception) {
            // No need to do anything here.
        } finally {
            closeSocket(socket.get());
        }

        System.exit(0);
    }


    private static Optional<Socket> createSocket(AtmCLIValidator input) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(input.getIpAddress(), input.getPort()), 10*1000);
            socket.setSoTimeout(10 * 1000);
            return Optional.ofNullable(socket);
        } catch (UnknownHostException e) {
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void replaceErrorPrintStream() {
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {/* NO-OP*/}
        });
        System.setErr(dummyStream);
    }

    private static void loadAuthFileEncryptionManager(String authFilenameWithExtension, String authFileLocation) {
        if (StringUtils.isBlank(authFileLocation)) {
            authFileLocation = System.getProperty("user.dir");
        }

        try {
            String file = authFileLocation;
            if (!file.endsWith(File.separator)) {
                file = file + File.separator;
            }
            file = file + authFilenameWithExtension;
            String content = new String(Files.readAllBytes(Paths.get(file)));
            EncryptionManager.initialize(content);
        } catch (Exception failure) {
            System.exit(1);
        }
    }

    private static Request generateInitialRequest(String accountName, String card, ModeOfOperation requestType, double amount) {
        return new Request(UUID.randomUUID(), accountName, card, TransactionType.valueOf(requestType.name()), BigDecimal.valueOf(amount));
    }

    private static String getAuthFileLocation(String[] args) {
        String authFileLoc = null;
        if (args.length >= 1) {
            authFileLoc = args[args.length - 1];
            // Check if authFileLocation is valid
            if (!new File(authFileLoc).isDirectory()) {
                authFileLoc = null;
            }
        }
        return authFileLoc;
    }

    private static AtmCLIValidator parseProgramArguments(String[] args) {
        try {
            return new AtmCLIValidator(args);
        } catch (ParseException e) {
            return null;
        }
    }

    private static void handleFailure(Exception ex) {
        if(ex instanceof JsonProcessingException) {
            System.exit(255);

        } else if(ex instanceof BadPaddingException) {
            System.exit(255);

        } else if(ex instanceof IllegalBlockSizeException) {
            System.exit(255);

        } else if(ex instanceof SocketTimeoutException) {
            System.exit(63);

        } else if(ex instanceof SocketException) {
            System.exit(63);

        } else {
            System.exit(255);

        }
    }

    private static void sendDataToSocket(Socket socket, byte[] dataToSend) throws IOException {
        socket.getOutputStream().write(dataToSend);
        socket.getOutputStream().write(END_OF_STREAM);
        socket.getOutputStream().flush();
    }

    public static byte[] readDataFromSocket(Socket socket) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = socket.getInputStream().read()) != END_OF_STREAM) {
                baos.write(b);
                if(baos.size() > 2048) {
                    return null;
                }
            }
            byte[] data = baos.toByteArray();
            baos.flush();
            baos.close();
            return data;
        } catch (IOException e) {
            return null;
        }
    }

    private static void handleErrorResponse(Response response, Socket socket) {
        switch (response.getError()) {
            case INSUFFICIENT_BALANCE:
            case DUPLICATE_USER:
            case USER_AUTHENTICATION_FAILED:
            case USER_NOT_PRESENT:
            case INVALID_REQUEST:
            case BAD_REQUEST:
                Try.run(() -> socket.close()).andFinally(() -> System.exit(255));
                break;
            case PROTOCOL_ERROR:
                Try.run(() -> socket.close()).andFinally(() -> System.exit(63));
                break;

        }
    }

    public static void handleSuccessfulResponse(Response response) {
        switch (response.getResponseType()) {
            case DEPOSIT:
                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                        .append("\"").append("deposit").append("\":").append(response.getAmount()).append("}")
                        .toString());
                break;
            case CREATE_ACCOUNT:
                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                        .append("\"").append("initial_balance").append("\":").append(response.getAmount()).append("}")
                        .toString());
                break;
            case WITHDRAW:
                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                        .append("\"").append("withdraw").append("\":").append(response.getAmount()).append("}")
                        .toString());
                break;
            case GET_BALANCE:
                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                        .append("\"").append("balance").append("\":").append(response.getAmount()).append("}")
                        .toString());
                break;
            case PROTOCOL_ERROR:
                //TODO What should be done here.
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {}
    }
}
