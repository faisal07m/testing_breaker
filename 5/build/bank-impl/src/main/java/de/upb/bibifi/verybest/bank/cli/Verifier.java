package de.upb.bibifi.verybest.bank.cli;

import de.upb.bibifi.verybest.common.util.InputValidationUtil;

public class Verifier {
    public static boolean isValid(CommandLineArgs args) {
        boolean portValid = InputValidationUtil.isPortValid(args.port());
        if (!portValid) {
            System.err.println("Port is invalid!");
            return false;
        }

        boolean validFilename = InputValidationUtil.isValidFilename(args.authFile().getName());
        if (!validFilename) {
            System.err.println("Auth file name is invalid!");
            return false;
        }

        if (args.authFile().exists()) {
            System.err.println("Auth file already exists");
            return false;
        }

        return true;
    }
}
