package de.upb.bibifi.verybest.bank;

import de.upb.bibifi.verybest.bank.cli.CommandLineArgs;
import de.upb.bibifi.verybest.bank.cli.CommandLineReader;
import de.upb.bibifi.verybest.bank.cli.CommandLineReaderImpl;
import de.upb.bibifi.verybest.bank.communication.impl.BankProtocolHandlerImpl;
import de.upb.bibifi.verybest.bank.communication.impl.BankSocketHandler;
import de.upb.bibifi.verybest.bank.communication.interfaces.IBankProtocolHandler;
import de.upb.bibifi.verybest.bank.worker.Worker;
import de.upb.bibifi.verybest.bank.worker.WorkerImpl;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.communication.interfaces.IAuthFileGenerator;
import de.upb.bibifi.verybest.common.communication.interfaces.IEncryptionSigningHandler;
import de.upb.bibifi.verybest.common.communication.interfaces.impl.AuthFileGeneratorImpl;
import de.upb.bibifi.verybest.common.communication.interfaces.impl.EncryptionSigningHandlerImpl;
import de.upb.bibifi.verybest.common.exception.CryptoInitException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.function.Supplier;

public class Main {

    public static final String PROTOCOL_ERROR = "protocol_error";

    public static void main(String[] args) {
        System.exit(mainWithExitCode(args));
    }

    private static int mainWithExitCode(String[] args) {
        CommandLineReader commandLineReader = new CommandLineReaderImpl();
        Pair<CommandLineArgs, Integer> argsPair = commandLineReader.parseArgs(args);
        if (!argsPair.getRight().equals(0)) {
            return argsPair.getRight();
        }

        CommandLineArgs parsedArgs = argsPair.getLeft();

        IAuthFileGenerator authFileGenerator = new AuthFileGeneratorImpl();
        try {
            authFileGenerator.generateAuthFile(parsedArgs.authFile());
            System.out.println("created");
        } catch (IOException e) {
            System.err.println("Error while generating auth file.");
            e.printStackTrace();
            return Constants.ERROR_CODE_USER;
        }

        Worker worker = new WorkerImpl();
        IEncryptionSigningHandler signingHandler;
        try {
            signingHandler = new EncryptionSigningHandlerImpl(parsedArgs.authFile());
        } catch (CryptoInitException e) {
            System.err.println("the initialization of the cryptographic layer failed");
            e.printStackTrace();
            return Constants.ERROR_CODE_USER;
        }

        Supplier<IBankProtocolHandler> bankProtocolHandlerSupplier = () -> {
            BankProtocolHandlerImpl bankProtocolHandler = new BankProtocolHandlerImpl();
            bankProtocolHandler.init(signingHandler, worker);
            return bankProtocolHandler;
        };

        // Bind to all interfaces with 0.0.0.0
        BankSocketHandler handler = new BankSocketHandler("0.0.0.0", parsedArgs.port(), bankProtocolHandlerSupplier);
        try {
            handler.start();
            // To avoid direct exit of the program, since the server is running in another thread
            // noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (IOException e) {
            throw new AssertionError("Socket is in use.", e);
        }

        return 0;
    }

}
