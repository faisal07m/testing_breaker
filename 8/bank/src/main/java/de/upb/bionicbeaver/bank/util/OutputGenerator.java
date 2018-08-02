package de.upb.bionicbeaver.bank.util;

import de.upb.bionicbeaver.bank.model.Response;

public class OutputGenerator {

    public static void generateOutput(Response response) {
        if(response != Response.EMPTY_RESPONSE && response.getError() == null) {
            switch (response.getResponseType()) {
                case CREATE_ACCOUNT:
                    System.out.println(new StringBuilder()
                            .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                            .append("\"").append("initial_balance").append("\":").append(response.getAmount()).append("}")
                            .toString());
                    break;
                case DEPOSIT:
                    System.out.println(new StringBuilder()
                            .append("{\"").append("account").append("\":\"").append(response.getAccountName()).append("\",")
                            .append("\"").append("deposit").append("\":").append(response.getAmount()).append("}")
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
                    System.out.println("protocol_error");
            }
        }
    }
}
