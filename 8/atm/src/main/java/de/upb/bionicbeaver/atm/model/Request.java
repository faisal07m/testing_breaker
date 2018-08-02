package de.upb.bionicbeaver.atm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Jerome Kempf, Thomas Wiens
 * @since 2018-07-26
 */
@Getter
public class Request {
    private final UUID id;
    private final String accountName;
    private final String card;
    private final TransactionType requestType;
    private final BigDecimal amount;

    @JsonCreator
    public Request(@JsonProperty("id") UUID id,
                   @JsonProperty("accountName") String accountName,
                   @JsonProperty("card") String card,
                   @JsonProperty("requestType") TransactionType requestType,
                   @JsonProperty("amount") BigDecimal amount) {
        this.id = id;
        this.accountName = accountName;
        this.card = card;
        this.requestType = requestType;
        this.amount = amount;
    }

    public double getAmount() {
        if(Objects.nonNull(amount))
            return amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        else
            return -1.0;
    }
}
