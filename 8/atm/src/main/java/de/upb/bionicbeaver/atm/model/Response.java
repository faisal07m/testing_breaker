package de.upb.bionicbeaver.atm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class Response {
    @Getter
    private final UUID id;
    @Getter
    private final String accountName;
    @Getter
    private final TransactionType responseType;
    private final BigDecimal amount;
    @Getter
    private final Error error;

    @JsonCreator
    public Response(@JsonProperty("id") UUID id,
                    @JsonProperty("accountName") String accountName,
                    @JsonProperty("responseType") TransactionType responseType,
                    @JsonProperty("amount") Double amount,
                    @JsonProperty("error") Error error) {
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
        error = null;
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
