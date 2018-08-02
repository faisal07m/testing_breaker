package de.upb.bibifi.verybest.common.models;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public interface CLISummary {

    static BigDecimal toOutputBigDecimal(BigInteger cents) {
        return new BigDecimal(cents).movePointLeft(2);
    }

    static BigDecimal toOutputBigDecimal(BigDecimal euros) {
        return euros.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigInteger fromInputBigDecimal(BigDecimal euros) {
        return euros.movePointRight(2).toBigIntegerExact();
    }

    default String toJson() {
        if (this instanceof CreateAccount) {
            return CLISummaryMoshi.createAccountSuccessAdapter.toJson((CreateAccount) this);
        } else if (this instanceof Deposit) {
            return CLISummaryMoshi.depositSuccessAdapter.toJson((Deposit) this);
        } else if (this instanceof GetBalance) {
            return CLISummaryMoshi.getBalanceSuccessAdapter.toJson((GetBalance) this);
        } else if (this instanceof Withdraw) {
            return CLISummaryMoshi.withdrawSuccessAdapter.toJson((Withdraw) this);
        } else {
            throw new AssertionError("Unhandled success type!");
        }
    }

    class CreateAccount implements CLISummary {
        private final String account;
        @Json(name = "initial_balance")
        private final BigDecimal initialBalance;

        public CreateAccount(String account, BigDecimal initialBalance) {
            this.account = account;
            this.initialBalance = initialBalance;
        }
    }

    class Deposit implements CLISummary {
        private final String account;
        private final BigDecimal deposit;

        public Deposit(String account, BigDecimal deposit) {
            this.account = account;
            this.deposit = deposit;
        }
    }

    class Withdraw implements CLISummary {
        private final String account;
        private final BigDecimal withdraw;

        public Withdraw(String account, BigDecimal withdraw) {
            this.account = account;
            this.withdraw = withdraw;
        }
    }

    class GetBalance implements CLISummary {
        private final String account;
        private final BigDecimal balance;

        public GetBalance(String account, BigDecimal balance) {
            this.account = account;
            this.balance = balance;
        }
    }
}

class CLISummaryMoshi {
    private static final Moshi moshi = new Moshi.Builder().add(BigDecimal.class, new BigDecimalAdapter()).build();
    static final JsonAdapter<CLISummary.CreateAccount> createAccountSuccessAdapter =
            moshi.adapter(CLISummary.CreateAccount.class);
    static final JsonAdapter<CLISummary.Deposit> depositSuccessAdapter =
            moshi.adapter(CLISummary.Deposit.class);
    static final JsonAdapter<CLISummary.GetBalance> getBalanceSuccessAdapter =
            moshi.adapter(CLISummary.GetBalance.class);
    static final JsonAdapter<CLISummary.Withdraw> withdrawSuccessAdapter =
            moshi.adapter(CLISummary.Withdraw.class);

    private static class BigDecimalAdapter extends JsonAdapter<BigDecimal> {

        @Nullable @Override public BigDecimal fromJson(JsonReader jsonReader) throws IOException {
            Object jsonValue = jsonReader.readJsonValue();
            if (jsonValue instanceof BigDecimal) {
                return (BigDecimal) jsonValue;
            } else if (jsonValue == null) {
                return null;
            } else {
                throw new JsonDataException("Unexpected value of class " + jsonValue.getClass());
            }
        }

        @Override public void toJson(JsonWriter jsonWriter, @Nullable BigDecimal bigDecimal) throws IOException {
            jsonWriter.value(bigDecimal);
        }
    }
}