package de.upb.bionicbeaver.bank.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Jerome Kempf, Thomas Wiens
 * @since 2018-07-26
 */
public class Request {
    @Getter
    private final UUID id;
    @Getter
    private final String accountName;
    @Getter
    private final String card;
    @Getter
    private final TransactionType requestType;
    private final BigDecimal amount;

    @JsonCreator
    public Request(@JsonProperty("id") UUID id,
                   @JsonProperty("accountName") String accountName,
                   @JsonProperty("card") String card,
                   @JsonProperty("requestType") TransactionType requestType,
                   @JsonProperty("amount") Double amount) {
        this.id = id;
        this.accountName = accountName;
        this.card = card;
        this.requestType = requestType;
        if(Objects.nonNull(amount) && amount >= 0)
            this.amount = BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        else
            this.amount = null;
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
