package de.upb.bionicbeaver.bank;

import de.upb.bionicbeaver.bank.handler.IncomingRequestProcessingHandler;
import de.upb.bionicbeaver.bank.validation.BankCLIValidator;
import io.vavr.control.Try;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Main class to start bank app.
 */
public class Bank {

    public static void main(String args[]) {

        replaceErrorPrintStream();
        // Parse the input args
        final BankCLIValidator inputArgs = parseProgramArguments(args);
        // The last argument is the location where the auth file must be placed.
        final String authFileLoc = getAuthFileLocation(args);
        // Init thread pool
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        // Execute security manager that writes the auth file
        executorService.execute(new SecurityManager(inputArgs.getAuthFileName(), authFileLoc));

        // Start socket server
        ServerSocket server = Try.of(() ->  new ServerSocket(inputArgs.getPort()))
                .onFailure(failure -> {
                    executorService.shutdownNow();
                    System.exit(255);
                }).get();

        // Listen for incoming connection requests from client
        while (true) {
            Try.run(() -> executorService.execute(new IncomingRequestProcessingHandler(server.accept())))
                    .onFailure(failure -> System.out.println("protocol_error"))
                    .get();
        }


        /*
        // Adds shutdown hook to ensure propoer handling of SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                //System.out.println("Someone called shutdown hook.");
                // Close Socket server
                Try.run(() -> {
                    if(Objects.nonNull(futures.get(0))
                            && Objects.nonNull(futures.get(0).get())
                            && futures.get(0).get() instanceof SocketServer) {
                        closeGracefully(futures.get(0).get());
                    } else if(Objects.nonNull(futures.get(1))
                            && Objects.nonNull(futures.get(1).get())
                            && futures.get(1).get() instanceof SocketServer) {
                        closeGracefully(futures.get(1).get());
                    }
                    System.exit(0);
                }).onFailure(failure -> {
                    System.err.println("Cannot gracefully stop Socket Server. Cause: " + failure.getMessage());
                    System.exit(0);
                });

                // Close thread pool
                Try.run(() -> executorService.shutdownNow())
                        .onFailure(failure -> {
                            System.err.println("Cannot gracefully stop Thread pool. Cause: " + failure.getMessage());
                            System.exit(0);
                        });
                System.exit(0);
            }

            private void closeGracefully(Initiator initiator) {
                SocketServer socketSrv = (SocketServer) initiator;
                socketSrv.close();
            }
        }));
        */
    }

    private static void replaceErrorPrintStream() {
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {/* NO-OP*/}
        });
        System.setErr(dummyStream);
    }

    private static BankCLIValidator parseProgramArguments(String[] args) {
        return Try.of(() -> new BankCLIValidator(args))
                .onFailure(failure -> System.exit(255))
                .get();
    }

    private static String getAuthFileLocation(String[] args) {
        String authFileLoc = null;
        if(args.length >= 1) {
            authFileLoc = args[args.length - 1];
            // Check if authFileLocation is valid
            if (!new File(authFileLoc).isDirectory()) {
                authFileLoc = null;
            }
        }
        return authFileLoc;
    }
}
