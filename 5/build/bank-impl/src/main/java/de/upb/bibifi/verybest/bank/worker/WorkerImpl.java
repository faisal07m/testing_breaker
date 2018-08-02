package de.upb.bibifi.verybest.bank.worker;

import de.upb.bibifi.verybest.common.models.*;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class WorkerImpl implements Worker {

    private static final BigInteger MIN_CREATE_BALANCE = BigInteger.valueOf(1000);

    private final Map<String, Account> accountByName = new LinkedHashMap<>();
    private final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());

    @Nullable
    private volatile Thread lastThread;

    // WARNING: It is important that each method here only passes a single
    // action to the scheduler, e.g. one `map` action. If multiple actions
    // per method are used, the method is not atomic anymore.

    @Override
    public Single<WorkerResult> enqueue(Action action) {
        return Single.just(action)
                .observeOn(scheduler)
                .map(this::handleAction);
    }

    @Override
    public Single<Optional<byte[]>> publicKeyForAccountName(String accountName) {
        return Single.just(accountName)
                .observeOn(scheduler)
                .map(aName ->
                        Optional.ofNullable(accountByName.getOrDefault(aName, null))
                                .map(Account::publicKey)
                );
    }

    private WorkerResult handleAction(Action action) {
        ensureSingleThread();

        if (action instanceof AccountCreateAction) {
            return handleAccountCreateAction((AccountCreateAction) action);
        } else if (action instanceof DepositAction) {
            return handleDepositAction((DepositAction) action);
        } else if (action instanceof GetBalanceAction) {
            return handleGetBalanceAction((GetBalanceAction) action);
        } else if (action instanceof WithdrawAction) {
            return handleWithdrawAction((WithdrawAction) action);
        } else {
            throw new IllegalArgumentException("Unknown action type " + action);
        }
    }

    private WorkerResult handleWithdrawAction(WithdrawAction action) {
        Account currentAccountStatus = accountByName.get(action.name());
        if (currentAccountStatus == null) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.AccountDoesNotExist));
        }

        if (action.amount().signum() < 1) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.NonPositiveWithdraw));
        }

        BigInteger newBalance = currentAccountStatus.balance().subtract(action.amount());
        if (newBalance.signum() == -1) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.Overdraft));
        }

        CLISummary.Withdraw withdraw = new CLISummary.Withdraw(action.name(), CLISummary.toOutputBigDecimal(action.amount()));
        System.out.println(withdraw.toJson());

        Account newAccountStatus = currentAccountStatus.toBuilder()
                .balance(newBalance)
                .build();
        accountByName.put(action.name(), newAccountStatus);
        return WorkerResult.success(WorkerResult.Success.create(newAccountStatus));
    }

    private WorkerResult handleGetBalanceAction(GetBalanceAction action) {
        Account currentAccountStatus = accountByName.get(action.name());
        if (currentAccountStatus == null) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.AccountDoesNotExist));
        }

        CLISummary.GetBalance getBalance = new CLISummary.GetBalance(action.name(), CLISummary.toOutputBigDecimal(currentAccountStatus.balance()));
        System.out.println(getBalance.toJson());

        return WorkerResult.success(WorkerResult.Success.create(currentAccountStatus));
    }

    private WorkerResult handleDepositAction(DepositAction action) {
        Account currentAccountStatus = accountByName.get(action.name());
        if (currentAccountStatus == null) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.AccountDoesNotExist));
        }

        if (action.amount().signum() < 1) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.NonPositiveDeposit));
        }

        CLISummary.Deposit deposit = new CLISummary.Deposit(action.name(), CLISummary.toOutputBigDecimal((action).amount()));
        System.out.println(deposit.toJson());

        Account newAccountStatus = currentAccountStatus.toBuilder()
                .balance(currentAccountStatus.balance().add(action.amount()))
                .build();
        accountByName.put(action.name(), newAccountStatus);
        return WorkerResult.success(WorkerResult.Success.create(newAccountStatus));
    }

    private WorkerResult handleAccountCreateAction(AccountCreateAction action) {
        if (accountByName.containsKey(action.name())) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.AccountAlreadyExists));
        }
        if (action.initialBalance().compareTo(MIN_CREATE_BALANCE) < 0) {
            return WorkerResult.failure(WorkerResult.Failure.create(WorkerResult.Failure.Reason.CreateBalanceTooLow));
        }

        CLISummary.CreateAccount createAccount = new CLISummary.CreateAccount(action.name(),
                CLISummary.toOutputBigDecimal(action.initialBalance()));
        System.out.println(createAccount.toJson());

        Account account = Account.create(
                action.name(),
                action.publicKey(),
                action.initialBalance()
        );
        accountByName.put(action.name(), account);
        return WorkerResult.success(WorkerResult.Success.create(account));
    }

    private void ensureSingleThread() {
        if (lastThread == null) {
            lastThread = Thread.currentThread();
        } else if (lastThread != Thread.currentThread()) {
            throw new AssertionError("Worker must run single-threaded!");
        }
    }
}
