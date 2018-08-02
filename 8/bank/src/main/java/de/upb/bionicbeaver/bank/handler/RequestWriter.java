package de.upb.bionicbeaver.bank.handler;

import de.upb.bionicbeaver.bank.model.Request;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static java.math.BigDecimal.ROUND_HALF_UP;

/**
 * Writes the response into a valid server stdout output.
 *
 * @author Siddhartha Moitra
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestWriter {

    private static final RequestWriter INSTANCE = new RequestWriter();

    public static final RequestWriter getInstance() {
        return INSTANCE;
    }

    public void write(Request request) {
        switch (request.getRequestType()) {
            case CREATE_ACCOUNT:

                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(request.getAccountName()).append("\",")
                        .append("{\"").append("initial_balance").append("\":").append(request.getAmountAsBigDecimal().setScale(2,ROUND_HALF_UP).doubleValue()).append("}")
                        .toString());
                break;

            case GET_BALANCE:

                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(request.getAccountName()).append("\",")
                        .append("{\"").append("balance").append("\":").append(request.getAmountAsBigDecimal().setScale(2,ROUND_HALF_UP).doubleValue()).append("}")
                        .toString());
                break;

            case DEPOSIT:

                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(request.getAccountName()).append("\",")
                        .append("{\"").append("deposit").append("\":").append(request.getAmountAsBigDecimal().setScale(2,ROUND_HALF_UP).doubleValue()).append("}")
                        .toString());
                break;

            case WITHDRAW:

                System.out.println(new StringBuilder()
                        .append("{\"").append("account").append("\":\"").append(request.getAccountName()).append("\",")
                        .append("{\"").append("withdraw").append("\":").append(request.getAmountAsBigDecimal().setScale(2,ROUND_HALF_UP).doubleValue()).append("}")
                        .toString());
                break;
        }
    }
}
