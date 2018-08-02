package de.upb.bibifi.verybest.atm.cli;

import javax.annotation.Nullable;
import java.io.File;
import java.math.BigDecimal;

public class CommandLineArgsImpl implements CommandLineArgs {

    private final String account;
    private final Action action;
    private final int port;
    private final File cardFile;
    private final String ipAddress;
    private final File authFile;

    CommandLineArgsImpl(
            String account,
            @Nullable Integer port,
            @Nullable File cardFile,
            @Nullable String ipAddress,
            @Nullable File authFile,
            Action action
    ) {
        this.account = account;
        this.action = action;
        if (port != null) {
            this.port = port;
        } else {
            this.port = 3000;
        }
        if (cardFile == null) {
            this.cardFile = new File(this.account + ".card");
        } else {
            this.cardFile = cardFile;
        }
        if ("".equals(ipAddress) || ipAddress == null) {
            this.ipAddress = "127.0.0.1";
        } else {
            this.ipAddress = ipAddress;
        }
        if (authFile == null) {
            this.authFile = new File("bank.auth");
        } else {
            this.authFile = authFile;
        }
    }

    static class GetAccountBalanceActionImpl implements GetAccountBalanceAction {
    }

    static class DepositActionImpl implements DepositAction {
        private final BigDecimal amount;

        DepositActionImpl(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        public BigDecimal amount() {
            return amount;
        }
    }

    static class WithdrawActionImpl implements WithdrawAction {
        private final BigDecimal amount;

        WithdrawActionImpl(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        public BigDecimal amount() {
            return amount;
        }
    }

    static class NewAccountActionImpl implements NewAccountAction {
        private final BigDecimal initialBalance;

        NewAccountActionImpl(BigDecimal initialBalance) {
            this.initialBalance = initialBalance;
        }

        @Override
        public BigDecimal initialBalance() {
            return initialBalance;
        }
    }

    @Override
    public File authFile() {
        return authFile;
    }

    @Override
    public String ipAddress() {
        return ipAddress;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public File cardFile() {
        return cardFile;
    }

    @Override
    public String account() {
        return account;
    }

    @Override
    public Action action() {
        return action;
    }

}