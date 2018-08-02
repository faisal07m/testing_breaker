package de.upb.bionicbeaver.bank.handler;

import de.upb.bionicbeaver.bank.encryption.EncryptionManager;
import de.upb.bionicbeaver.bank.exception.Error;
import de.upb.bionicbeaver.bank.exception.NoAckRequiredException;
import de.upb.bionicbeaver.bank.exception.ServerException;
import de.upb.bionicbeaver.bank.model.Request;
import de.upb.bionicbeaver.bank.model.Response;
import de.upb.bionicbeaver.bank.util.JsonMapperProvider;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Objects;

/**
 * Handles all the failures due to transaction processing or other kinds of errors.
 *
 * @author Siddhartha Moitra
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FailureResponseHandler {

    private static final FailureResponseHandler INSTANCE = new FailureResponseHandler();

    public static FailureResponseHandler getInstance() {
        return INSTANCE;
    }

    public byte[] handle(Throwable throwable, Request request) {
        if (throwable instanceof NoAckRequiredException) {
            // Nothing to do here.
            return null;
        } else if (throwable instanceof ServerException) {
            ServerException serverException = (ServerException) throwable;
            switch (serverException.getError()) {
                case REQUEST_REPLAY:
                    break;
                default:
                    Error error = serverException.getError();
                    // Create error response
                    Response response;
                    if(Objects.nonNull(request)) {
                        response = new Response(request.getId(), request.getAccountName(), request.getRequestType(), request.getAmount(), error);
                    } else {
                        response = Response.createErrorResponse(error);
                    }
                    byte[] responseArr = Try.of(() -> JsonMapperProvider.getInstance().get().writeValueAsBytes(response))
                            .onFailure(failure -> System.err.println("app_error"))
                            .get();

                    return Try.of(() -> EncryptionManager.getInstance().encrypt(responseArr))
                            .onFailure(failure -> System.err.println("app_error"))
                            .get();

            }
            return null;
        } else if(throwable instanceof SocketTimeoutException || throwable instanceof SocketException) {
            System.out.println("protocol_error");
            return null;
        } else {
            Response response = Response.createErrorResponse(Error.PROTOCOL_ERROR);
            byte[] responseArr = Try.of(() -> JsonMapperProvider.getInstance().get().writeValueAsBytes(response))
                    .onFailure(failure -> System.err.println("app_error"))
                    .get();

            return Try.of(() -> EncryptionManager.getInstance().encrypt(responseArr))
                    .onFailure(failure -> System.err.println("app_error"))
                    .get();

        }
    }
}
