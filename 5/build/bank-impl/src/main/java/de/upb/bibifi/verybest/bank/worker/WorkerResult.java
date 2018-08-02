package de.upb.bibifi.verybest.bank.worker;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import de.upb.bibifi.verybest.common.models.Account;

@AutoOneOf(WorkerResult.Kind.class)
public abstract class WorkerResult {

    public enum Kind {
        Success, Failure
    }

    public static WorkerResult success(Success success) {
        return AutoOneOf_WorkerResult.success(success);
    }

    public static WorkerResult failure(Failure failure) {
        return AutoOneOf_WorkerResult.failure(failure);
    }

    public abstract Kind kind();

    public abstract Success success();

    public abstract Failure failure();

    @AutoValue
    public abstract static class Success {
        public static Success create(Account newAccountStatus) {
            return new AutoValue_WorkerResult_Success(newAccountStatus);
        }

        public abstract Account newAccountStatus();
    }

    @AutoValue
    public abstract static class Failure {

        public enum Reason {
            AccountDoesNotExist, AccountAlreadyExists, Overdraft, CreateBalanceTooLow,
            NonPositiveDeposit, NonPositiveWithdraw
        }

        public static Failure create(Reason reason) {
            return new AutoValue_WorkerResult_Failure(reason);
        }

        public abstract Reason reason();
    }
}
