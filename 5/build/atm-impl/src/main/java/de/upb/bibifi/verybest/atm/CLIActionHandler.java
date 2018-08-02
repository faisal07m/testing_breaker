package de.upb.bibifi.verybest.atm;

import com.squareup.moshi.JsonDataException;
import de.upb.bibifi.verybest.atm.cli.CommandLineArgs;
import de.upb.bibifi.verybest.atm.cli.CommandLineResponse;
import de.upb.bibifi.verybest.common.Constants;
import de.upb.bibifi.verybest.common.exception.CommunicationFailedException;
import de.upb.bibifi.verybest.common.exception.OperationFailedException;
import de.upb.bibifi.verybest.common.models.Account;
import de.upb.bibifi.verybest.common.models.AccountCreateActionTemplate;
import de.upb.bibifi.verybest.common.models.DepositAction;
import de.upb.bibifi.verybest.common.models.GetBalanceAction;
import de.upb.bibifi.verybest.common.models.WithdrawAction;
import de.upb.bibifi.verybest.communication.interfaces.ICommunicationHandler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.upb.bibifi.verybest.common.models.CLISummary.fromInputBigDecimal;
import static de.upb.bibifi.verybest.common.models.CLISummary.toOutputBigDecimal;

class CLIActionHandler {

    @FunctionalInterface
    interface ICommunicationHandlerProvider {
        ICommunicationHandler provide() throws OperationFailedException, CommunicationFailedException;
    }

    private final File userCard;
    private final String name;
    private final ICommunicationHandlerProvider communicationHandlerProvider;
    @Nullable
    private ICommunicationHandler lazyCommunicationHandler;

    CLIActionHandler(ICommunicationHandlerProvider communicationHandlerProvider, File userCard, String name) {
        this.communicationHandlerProvider = communicationHandlerProvider;
        this.userCard = userCard;
        this.name = name;
    }

    private synchronized ICommunicationHandler communicationHandler() throws OperationFailedException, CommunicationFailedException {
        ICommunicationHandler handler = lazyCommunicationHandler;
        if (handler == null) {
            handler = communicationHandlerProvider.provide();
            lazyCommunicationHandler = handler;
        }
        return handler;
    }

    CommandLineResponse handleActionWithTimeout(CommandLineArgs.Action action, long timeoutMs) {
        CommandLineResponse timeoutResponse = CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));

        try {
            CommandLineResponse commandLineResponse = Single.just(action)
                .observeOn(Schedulers.single())
                .map(this::handleAction)
                .toFuture()
                .get(timeoutMs, TimeUnit.MILLISECONDS);

            if (commandLineResponse.kind() == CommandLineResponse.Kind.Failure) {
                rollback(action);
            }

            return commandLineResponse;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            rollback(action);
            e.printStackTrace();
            return timeoutResponse;
        }
    }

    private void rollback(CommandLineArgs.Action action) {
        if (action instanceof CommandLineArgs.NewAccountAction) {
            System.err.println("Tried to create new account, but wasn't successful. Deleting card...");
            //noinspection ResultOfMethodCallIgnored
            userCard.delete();
        }
    }

    CommandLineResponse handleAction(CommandLineArgs.Action action) {
        try {
            if (action instanceof CommandLineArgs.DepositAction) {
                CommandLineArgs.DepositAction depositAction = (CommandLineArgs.DepositAction) action;
                return handleDepositAction(depositAction);
            } else if (action instanceof CommandLineArgs.GetAccountBalanceAction) {
                CommandLineArgs.GetAccountBalanceAction getAccountBalanceAction = (CommandLineArgs.GetAccountBalanceAction) action;
                return handleGetAccountBalanceAction(getAccountBalanceAction);
            } else if (action instanceof CommandLineArgs.NewAccountAction) {
                CommandLineArgs.NewAccountAction newAccountAction = (CommandLineArgs.NewAccountAction) action;
                return handleNewAccountAction(newAccountAction);
            } else if (action instanceof CommandLineArgs.WithdrawAction) {
                CommandLineArgs.WithdrawAction withdrawAction = (CommandLineArgs.WithdrawAction) action;
                return handleWithdrawAction(withdrawAction);
            } else {
                throw new AssertionError("Unhandled CommandLineArgs.Action type!");
            }
        } catch (JsonDataException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));
        }
    }

    private CommandLineResponse handleWithdrawAction(CommandLineArgs.WithdrawAction withdrawAction) {
        WithdrawAction action = new WithdrawAction(name, fromInputBigDecimal(withdrawAction.amount()));
        try {
            communicationHandler().withdraw(action, userCard);
            return CommandLineResponse.success(new CommandLineResponse.Success.Withdraw(name, toOutputBigDecimal(withdrawAction.amount())));
        } catch (OperationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_USER));
        } catch (CommunicationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));
        }
    }

    private CommandLineResponse handleNewAccountAction(CommandLineArgs.NewAccountAction newAccountAction) {
        AccountCreateActionTemplate action = AccountCreateActionTemplate.create(name, fromInputBigDecimal(newAccountAction.initialBalance()));
        try {
            Account newAccountStatus = communicationHandler().createAccount(action, userCard);
            return CommandLineResponse.success(new CommandLineResponse.Success.CreateAccount(name, toOutputBigDecimal(newAccountStatus.balance())));
        } catch (OperationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_USER));
        } catch (CommunicationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));
        }
    }

    private CommandLineResponse handleGetAccountBalanceAction(CommandLineArgs.GetAccountBalanceAction getAccountBalanceAction) {

        GetBalanceAction action = new GetBalanceAction(name);
        try {
            Account newAccountStatus = communicationHandler().get(action, userCard);
            return CommandLineResponse.success(new CommandLineResponse.Success.GetBalance(name, toOutputBigDecimal(newAccountStatus.balance())));
        } catch (OperationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_USER));
        } catch (CommunicationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));
        }
    }

    private CommandLineResponse handleDepositAction(CommandLineArgs.DepositAction depositAction) {
        try {
            DepositAction action = new DepositAction(name, fromInputBigDecimal(depositAction.amount()));
            communicationHandler().deposit(action, userCard);
            return CommandLineResponse.success(new CommandLineResponse.Success.Deposit(name, toOutputBigDecimal(depositAction.amount())));
        } catch (OperationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_USER));
        } catch (CommunicationFailedException e) {
            e.printStackTrace();
            return CommandLineResponse.failure(CommandLineResponse.Failure.create(Constants.ERROR_CODE_COMMUNICATION));
        }
    }

}
