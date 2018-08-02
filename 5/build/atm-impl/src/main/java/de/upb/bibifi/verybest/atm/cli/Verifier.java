package de.upb.bibifi.verybest.atm.cli;

import de.upb.bibifi.verybest.common.util.InputValidationUtil;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Verifier {
    public static boolean isValid(CommandLineArgs args) {

        File authFile = args.authFile();
        if (!InputValidationUtil.isValidFilename(authFile.getName())) {
            System.err.println("Invalid filename for auth file");
            return false;
        }
        if (!authFile.exists() || authFile.length() == 0) {
            System.err.println("Auth file does not exist or is empty");
            return false;
        }

        File cardFile = args.cardFile();
        if (!InputValidationUtil.isValidFilename(cardFile.getName())) {
            System.err.println("Invalid filename for card");
            return false;
        }
        if (args.action() instanceof CommandLineArgs.NewAccountAction) {
            if (cardFile.exists()) {
                System.err.println("Card file must not already exist");
                return false;
            }
        } else {
            if (!cardFile.exists() || cardFile.length() == 0) {
                System.err.println("Card file does not exist or is empty");
                return false;
            }
        }

        try {
            InetAddress host = Inet4Address.getByName(args.ipAddress());
            if (!(host instanceof Inet4Address)) {
                System.err.println("Host must be an IPv4 address");
                return false;
            }
        } catch (UnknownHostException e) {
            System.err.println("Invalid IP4 address");
            return false;
        }

        int port = args.port();
        if (!InputValidationUtil.isPortValid(port)) {
            System.err.println("Invalid port");
            return false;
        }

        String account = args.account();
        if (!InputValidationUtil.isValidAccountName(account)) {
            System.err.println("Invalid account name");
            return false;
        }

        CommandLineArgs.Action action = args.action();
        return actionIsValid(action);
    }

    private static boolean actionIsValid(CommandLineArgs.Action action) {
        if (action instanceof CommandLineArgs.NewAccountAction) {
            CommandLineArgs.NewAccountAction newAccountAction = (CommandLineArgs.NewAccountAction) action;
            boolean validNumber = InputValidationUtil.isValidNumber(newAccountAction.initialBalance());
            if (!validNumber) {
                System.err.println("Invalid number specified.");
            }
            return validNumber;
        } else if (action instanceof CommandLineArgs.DepositAction) {
            CommandLineArgs.DepositAction depositAction = (CommandLineArgs.DepositAction) action;
            boolean validNumber = InputValidationUtil.isValidNumber(depositAction.amount());
            if (!validNumber) {
                System.err.println("Invalid number specified.");
            }
            return validNumber;
        } else if (action instanceof CommandLineArgs.WithdrawAction) {
            CommandLineArgs.WithdrawAction withdrawAction = (CommandLineArgs.WithdrawAction) action;
            boolean validNumber = InputValidationUtil.isValidNumber(withdrawAction.amount());
            if (!validNumber) {
                System.err.println("Invalid number specified.");
            }
            return validNumber;
        }

        return true;
    }
}
