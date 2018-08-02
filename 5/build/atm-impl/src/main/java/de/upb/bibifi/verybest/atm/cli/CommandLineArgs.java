package de.upb.bibifi.verybest.atm.cli;

import java.io.File;
import java.math.BigDecimal;

public interface CommandLineArgs {
    interface Action {
    }

    interface GetAccountBalanceAction extends Action {
    }

    interface DepositAction extends Action {
        BigDecimal amount();
    }

    interface WithdrawAction extends Action {
        BigDecimal amount();
    }

    interface NewAccountAction extends Action {
        BigDecimal initialBalance();
    }

    File authFile();

    String ipAddress();

    int port();

    File cardFile();

    String account();

    Action action();
}