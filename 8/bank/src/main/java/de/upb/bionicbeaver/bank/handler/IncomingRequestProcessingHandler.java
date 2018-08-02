package de.upb.bionicbeaver.bank.handler;

import de.upb.bionicbeaver.bank.encryption.EncryptionManager;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.Response;
import de.upb.bionicbeaver.bank.tx.ReplayAttackValidationFunction;
import de.upb.bionicbeaver.bank.tx.TxProcessingFunction;
import de.upb.bionicbeaver.bank.tx.TxValidationFunction;
import de.upb.bionicbeaver.bank.util.JsonMapperProvider;
import de.upb.bionicbeaver.bank.util.OutputGenerator;
import de.upb.bionicbeaver.bank.validation.CLIValidator;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

/**
 * Processes the incoming requests.
 *
 * @author Siddhartha Moitra
 */
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class IncomingRequestProcessingHandler implements Runnable {

    private static final int END_OF_STREAM = 46;

    private final Socket socket;

    @Override
    public void run() {
        // Set socket timeout
        Try.run(() -> socket.setSoTimeout(10 * 1000));
        Request request = null;
        try {
            byte[] rawMessageFromClient = readBytesFromSocket();
            if(Objects.isNull(rawMessageFromClient)) {
                System.out.println("protocol_error");
                socket.close();
                return;
            }
            byte[] rawMessagesFromClientDecrypted = EncryptionManager.getInstance().decrypt(rawMessageFromClient);
            request = JsonMapperProvider.getInstance().get().readValue(rawMessagesFromClientDecrypted, Request.class);

            CLIValidator.isValidRequest(request);
            ReplayAttackValidationFunction.getInstance().validate(request);
            TxValidationFunction.getInstance().validate(request);

            Response response = TxProcessingFunction.getInstance().process(request);
            OutputGenerator.generateOutput(response);
            if (response != Response.EMPTY_RESPONSE) {
                byte[] rawResponnseByteArr = JsonMapperProvider.getInstance().get().writeValueAsBytes(response);
                byte[] rawResponseEncrypted = EncryptionManager.getInstance().encrypt(rawResponnseByteArr);
                writeToSocket(rawResponseEncrypted);
            } else {
                socket.close();
            }
        } catch (Exception e) {
            writeAndCloseSocket(FailureResponseHandler.getInstance().handle(e, request));
        }
    }


    private void closeSocket() {
        Try.run(() -> this.socket.close());
    }

    private void writeToSocket(byte[] msg) {
        if(Objects.nonNull(msg)) {
            Try.run(() -> {
                socket.getOutputStream().write(msg);
                socket.getOutputStream().write(END_OF_STREAM);
                socket.getOutputStream().flush();
            }).onFailure(failure -> {
                System.out.println("protocol_error");
                closeSocket();
            });
        } else {
            closeSocket();
        }
    }

    private void writeAndCloseSocket(byte[] msg) {
        writeToSocket(msg);
        closeSocket();
    }
    public byte[] readBytesFromSocket() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int b;
            while ((b = socket.getInputStream().read()) != END_OF_STREAM) {
                byteArrayOutputStream.write(b);
                if(byteArrayOutputStream.size() > 2048) {
                    return null;
                }
            }
            byte[] data = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            return data;
        } catch (IOException e) {
            return null;
        }
    }
}
