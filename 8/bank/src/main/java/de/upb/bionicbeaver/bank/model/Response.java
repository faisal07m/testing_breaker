package de.upb.bionicbeaver.bank.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.upb.bionicbeaver.bank.exception.Error;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class Response {
    public static final Response EMPTY_RESPONSE = new Response();

    @Getter
    private final UUID id;
    @Getter
    private final String accountName;
    @Getter
    private final TransactionType responseType;
    private final BigDecimal amount;
    @Getter
    private final Error error;

    public static Response createValidResponse(UUID id, String accountName, TransactionType responseType, Double amount) {
        return new Response(id, accountName, responseType, amount);
    }

    public static Response createErrorResponse(Error error) {
        return new Response(null, null, null, -1.0, error);
    }

    public static Response createEmptyResponse() {
        return EMPTY_RESPONSE;
    }


    private Response(UUID id, String accountName, TransactionType responseType, Double amount) {
        this(id, accountName, responseType, amount, null);
    }

    public Response(UUID id, String accountName, TransactionType responseType, Double amount, Error error) {
        this.id = id;
        this.accountName = accountName;
        this.responseType = responseType;
        if(Objects.nonNull(amount) && amount >= 0)
            this.amount = BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        else
            this.amount = null;

        this.error = error;
    }

    private Response() {
        id = null;
        accountName = null;
        responseType = null;
        amount = null;
        this.error = null;
    }

    public Double getAmount() {
        if(Objects.nonNull(amount)) {
            return amount.doubleValue();
        }
        return null;
    }

    @JsonIgnore
    public BigDecimal getAmountAsBigDecimal() {
        return amount;
    }


}
