package de.upb.bibifi.verybest.bank.communication.impl;

import de.upb.bibifi.verybest.bank.Main;
import de.upb.bibifi.verybest.bank.communication.interfaces.IBankProtocolHandler;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.ProtocolException;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class BankSocketHandler {

    private static final int MAX_QUEUE_LENGTH = 1000;
    private static final int MAXIMUM_ARRAY_SIZE = 10485760;
    private static final int MAXIMUM_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 10;

    private final ThreadLocal<IBankProtocolHandler> bankProtocolHandlerThreadLocal;
    private final ServerSocket serverSocket;
    private final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(MAXIMUM_THREAD_COUNT));
    private volatile boolean running = true;

    public BankSocketHandler(String hostname, int port, Supplier<IBankProtocolHandler> bankProtocolHandlerSupplier) {
        this.bankProtocolHandlerThreadLocal = ThreadLocal.withInitial(bankProtocolHandlerSupplier);
        try {
            this.serverSocket = new ServerSocket(port, MAX_QUEUE_LENGTH, InetAddress.getByName(hostname));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void start() {
        running = true;
        new Thread(() -> {
            while (running) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println(Main.PROTOCOL_ERROR);
                    e.printStackTrace();
                    continue;
                }

                Completable.fromAction(() -> {
                    BufferedSource source = Okio.buffer(Okio.source(socket));
                    BufferedSink sink = Okio.buffer(Okio.sink(socket));
                    socket.setSoTimeout(Constants.TIMEOUT_MS);

                    IBankProtocolHandler bankProtocolHandler = bankProtocolHandlerThreadLocal.get();
                    byte[] initBytes = bankProtocolHandler.performInitRequest();
                    sink.writeInt(initBytes.length);
                    sink.write(initBytes);
                    sink.flush();

                    int headerLength = Math.min(source.readInt(), MAXIMUM_ARRAY_SIZE);
                    int bodySignatureLength = Math.min(source.readInt(), MAXIMUM_ARRAY_SIZE);
                    int bodyLength = Math.min(source.readInt(), MAXIMUM_ARRAY_SIZE);
                    byte[] header = source.readByteArray(headerLength);
                    byte[] bodySignature = source.readByteArray(bodySignatureLength);
                    byte[] body = source.readByteArray(bodyLength);

                    byte[] responseBody = bankProtocolHandler.performRequest(header, bodySignature, body);
                    sink.writeInt(responseBody.length);
                    sink.write(responseBody);
                    sink.flush();
                    source.close();
                    sink.close();
                    source.close();
                })
                        .subscribeOn(scheduler)
                        .timeout(Constants.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .doOnError(throwable -> {
                            throwable.printStackTrace();
                            if (throwable instanceof TimeoutException || throwable instanceof ProtocolException || throwable instanceof IOException || throwable instanceof CommunicationFailedException) {
                                System.out.println(Main.PROTOCOL_ERROR);
                            }
                            try {
                                if (!socket.isClosed()) {
                                    socket.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        })
                        .subscribe();
            }
        }).start();
    }

    public void stop() {
        running = false;
    }

}
