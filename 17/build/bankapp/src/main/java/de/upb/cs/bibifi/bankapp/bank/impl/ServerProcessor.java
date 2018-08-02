package de.upb.cs.bibifi.bankapp.bank.impl;

import com.google.gson.Gson;
import de.upb.cs.bibifi.bankapp.bank.IServerProcessor;
import de.upb.cs.bibifi.commons.dto.CreationResponse;
import de.upb.cs.bibifi.commons.dto.Response;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;
import de.upb.cs.bibifi.commons.enums.RequestType;
import org.json.JSONObject;

import java.math.BigDecimal;

import static de.upb.cs.bibifi.commons.constants.AppConstants.*;


public class ServerProcessor implements IServerProcessor {

    private static ServerProcessor processor;

    private ServerProcessor() {

    }

    public static ServerProcessor getServerProcessor() {
        if (processor == null)
            processor = new ServerProcessor();
        return processor;
    }


    // Validate TransmissionPacket before injecting it here!
    public Response executeOperation(TransmissionPacket transmissionPacket) {
        Response output = null;
        switch (transmissionPacket.getRequestType()) {
            case CREATE:
                output = createAccount(transmissionPacket);
                break;
            case DEPOSIT:
                output = deposit(transmissionPacket);
                break;
            case WITHDRAW:
                output = withdraw(transmissionPacket);
                break;
            case CHECKBALANCE:
                output = checkBalance(transmissionPacket);
                break;

        }
        return output;

    }

    private Response createAccount(TransmissionPacket transmissionPacket) {
        String accountName = transmissionPacket.getProperty(KEY_ACCOUNT_NAME);
        String balance = transmissionPacket.getProperty(KEY_BALANCE);
        String pin = Bank.getBank().createBalance(accountName, balance);
        Response response;
        if (pin == null) {
            response = new Response("", 255);
        } else {
            response = buildResponse(RequestType.CREATE, transmissionPacket, pin);
        }
        return response;
    }

    private Response deposit(TransmissionPacket transmissionPacket) {
        String accountName = transmissionPacket.getProperty(KEY_ACCOUNT_NAME);
        String balance = transmissionPacket.getProperty(KEY_DEPOSITE);
        String pin = transmissionPacket.getProperty(KEY_PIN);
        boolean success = Bank.getBank().deposit(accountName, pin, balance);
        Response response;
        if (success) {
            response = buildResponse(RequestType.DEPOSIT, transmissionPacket, null);
        } else {
            response = new Response("", 255);
        }
        return response;
    }

    private Response checkBalance(TransmissionPacket transmissionPacket) {
        String accountName = transmissionPacket.getProperty(KEY_ACCOUNT_NAME);
        String pin = transmissionPacket.getProperty(KEY_PIN);
        BigDecimal balance = Bank.getBank().checkBalance(accountName, pin);
        Response response;
        if (balance.compareTo(new BigDecimal("-1")) == 0) {
            response = new Response("", 255);
        } else {
            response = buildResponse(RequestType.CHECKBALANCE, transmissionPacket, String.valueOf(balance));
        }
        return response;
    }

    private Response withdraw(TransmissionPacket transmissionPacket) {
        String accountName = transmissionPacket.getProperty(KEY_ACCOUNT_NAME);
        String balance = transmissionPacket.getProperty(KEY_WIHTDRAW);
        String pin = transmissionPacket.getProperty(KEY_PIN);
        boolean success = Bank.getBank().withdraw(accountName, pin, balance);
        Response response;
        if (success) {
            response = buildResponse(RequestType.WITHDRAW, transmissionPacket, null);
        } else {
            response = new Response("", 255);
        }
        return response;
    }

    private Response buildResponse(RequestType type, TransmissionPacket transmissionPacket, String data) {
        Response response = null;
        JSONObject obj = new JSONObject();
        String message;
        switch (type) {
            case CREATE:
                obj.put(KEY_ACCOUNT_NAME, transmissionPacket.getProperty(KEY_ACCOUNT_NAME));
                obj.put(KEY_INITIAL_BALANCE, new BigDecimal(transmissionPacket.getProperty(KEY_BALANCE)));
                message = obj.toString();
                response = new CreationResponse(message, 0, data);
                break;
            case DEPOSIT:
                obj.put(KEY_ACCOUNT_NAME, transmissionPacket.getProperty(KEY_ACCOUNT_NAME));
                obj.put(KEY_DEPOSITE, new BigDecimal(transmissionPacket.getProperty(KEY_DEPOSITE)));
                message = obj.toString();
                response = new Response(message, 0);
                break;
            case WITHDRAW:
                obj.put(KEY_ACCOUNT_NAME, transmissionPacket.getProperty(KEY_ACCOUNT_NAME));
                obj.put(KEY_WIHTDRAW, new BigDecimal(transmissionPacket.getProperty(KEY_WIHTDRAW)));
                message = obj.toString();
                response = new Response(message, 0);
                break;
            case CHECKBALANCE:
                obj.put(KEY_ACCOUNT_NAME, transmissionPacket.getProperty(KEY_ACCOUNT_NAME));
                obj.put(KEY_BALANCE, new BigDecimal(data));
                message = obj.toString();
                response = new Response(message, 0);
                break;
        }
        return response;
    }
}
