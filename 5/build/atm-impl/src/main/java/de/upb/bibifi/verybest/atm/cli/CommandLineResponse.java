package de.upb.bibifi.verybest.atm.cli;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import de.upb.bibifi.verybest.common.models.CLISummary;

import java.math.BigDecimal;

@AutoOneOf(CommandLineResponse.Kind.class)
public abstract class CommandLineResponse {
    public enum Kind {
        Success, Failure
    }

    public static CommandLineResponse success(Success success) {
        return AutoOneOf_CommandLineResponse.success(success);
    }

    public static CommandLineResponse failure(Failure failure) {
        return AutoOneOf_CommandLineResponse.failure(failure);
    }

    public abstract Kind kind();

    public abstract Success success();

    public abstract Failure failure();

    public interface Success extends CLISummary {
        class CreateAccount extends CLISummary.CreateAccount implements Success {
            public CreateAccount(String account, BigDecimal initialBalance) {
                super(account, initialBalance);
            }
        }

        class Deposit extends CLISummary.Deposit implements Success {
            public Deposit(String account, BigDecimal deposit) {
                super(account, deposit);
            }
        }

        class Withdraw extends CLISummary.Withdraw implements Success {
            public Withdraw(String account, BigDecimal withdraw) {
                super(account, withdraw);
            }
        }

        class GetBalance extends CLISummary.GetBalance implements Success {
            public GetBalance(String account, BigDecimal withdraw) {
                super(account, withdraw);
            }
        }
    }

    @AutoValue
    public static abstract class Failure {
        public static Failure create(int exitCode) {
            return new AutoValue_CommandLineResponse_Failure(exitCode);
        }

        public abstract int exitCode();
    }
}
