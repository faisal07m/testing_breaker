package de.upb.bibifi.verybest.atm;

import de.upb.bibifi.verybest.atm.cli.CommandLineArgs;
import de.upb.bibifi.verybest.atm.cli.CommandLineReader;
import de.upb.bibifi.verybest.atm.cli.CommandLineReaderImpl;
import de.upb.bibifi.verybest.atm.cli.CommandLineResponse;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.OperationFailedException;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.communication.impl.ATMCommunicationHandler;
import de.upb.bibifi.verybest.communication.interfaces.ICommunicationHandler;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }

    private static int mainWithExitCode(String[] args) {
        CommandLineReader commandLineReader = new CommandLineReaderImpl();

        CommandLineArgs parsedArgs;
        Pair<CommandLineArgs, Integer> argsWithCode = commandLineReader.parseArgs(args);
        if (argsWithCode.getRight().equals(0)) {
            parsedArgs = argsWithCode.getLeft();
        } else {
            return argsWithCode.getRight();
        }
        CommandLineArgs.Action action = parsedArgs.action();

        Inet4Address host;
        try {
            host = (Inet4Address) Inet4Address.getByName(parsedArgs.ipAddress());
        } catch (UnknownHostException e) {
            throw new AssertionError("Should not happen after Verifier.isValid", e);
        }

        if (!(action instanceof CommandLineArgs.NewAccountAction)) {
            try {
                checkCardFile(parsedArgs.cardFile(), parsedArgs.account());
            } catch (OperationFailedException | CommunicationFailedException e) {
                System.err.println("Invalid user-card or altered user-card!");
                return Constants.ERROR_CODE_USER;
            }
        }

        CLIActionHandler.ICommunicationHandlerProvider communicationHandlerProvider =
            () -> provideCommunicationHandler(parsedArgs.authFile(), host, parsedArgs.port());
        CLIActionHandler cliActionHandler = new CLIActionHandler(communicationHandlerProvider, parsedArgs.cardFile(), parsedArgs.account());
        CommandLineResponse commandLineResponse = cliActionHandler.handleActionWithTimeout(action, Constants.TIMEOUT_MS);

        switch (commandLineResponse.kind()) {
            case Success:
                System.out.println(commandLineResponse.success().toJson());
                return 0;
            case Failure:
                return commandLineResponse.failure().exitCode();
            default:
                AssertionError up = new AssertionError("Unhandled kind!");
                throw up;
        }
    }

    private static ICommunicationHandler provideCommunicationHandler(File authFile, Inet4Address host, int port) throws OperationFailedException, CommunicationFailedException {
        ATMCommunicationHandler handler = new ATMCommunicationHandler();
        handler.initialize(authFile, host, port);
        return handler;
    }

    private static void checkCardFile(File file, String userName) throws OperationFailedException, CommunicationFailedException {
        try (final BufferedSource source = Okio.buffer(Okio.source(file))) {
            // 1 byte can store lengths up to 255
            int nameLength = (int) source.readByte();
            if (nameLength <= 0 || nameLength > 122) {
                //Non-compliant name length (altered user card!)
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
            byte[] nameBytes = new byte[nameLength];
            int readBytes = source.read(nameBytes);
            if (readBytes != nameLength) {
                //to few bytes
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
            String userCardName = new String(nameBytes, StandardCharsets.UTF_8);
            if (!userCardName.equals(userName)) {
                throw new OperationFailedException(new IllegalArgumentException("Altered user-card"));
            }
        } catch (final IOException exception) {
            throw new CommunicationFailedException(exception);
        }
    }
}
