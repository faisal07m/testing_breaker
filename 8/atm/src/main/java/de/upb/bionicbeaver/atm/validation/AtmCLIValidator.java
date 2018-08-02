package de.upb.bionicbeaver.atm.validation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.HashSet;

public final class AtmCLIValidator {

    private String account;
    private String authFile;
    private String ipAddress;
    private int port;
    private String cardFile;
    private ModeOfOperation modeOfOperation;
    private double mooValue;


    public AtmCLIValidator(String[] args) throws ParseException {

        /**
         * Specify command line options.
         */
        Option opt_s = new Option("s", true, "Auth file");
        Option opt_i = new Option("i", true, "IP address");
        Option opt_p = new Option("p", true, "Port");
        Option opt_c = new Option("c", true, "Card file");
        Option opt_a = new Option("a", true, "Account name");
        opt_a.setRequired(true);
        opt_a.setArgs(1);

        // Mode of operations.
        Option opt_n = new Option("n", true, "New account");
        Option opt_w = new Option("w", true, "Withdraw");
        Option opt_d = new Option("d", true, "Deposit");
        Option opt_g = new Option("g", false, "Current balance");
        opt_g.setArgs(0);

        Options posixOptions = new Options();
        posixOptions
                .addOption(opt_s)
                .addOption(opt_i)
                .addOption(opt_p)
                .addOption(opt_c)
                .addOption(opt_a)
                .addOption(opt_n)
                .addOption(opt_w)
                .addOption(opt_d)
                .addOption(opt_g);

        CommandLineParser posixParser = new DefaultParser();
        // Stops parsing when an unexpected token occurs. (Therefore error messages are not exact when unexpected tokens are used.)
        CommandLine cmd = posixParser.parse(posixOptions, args);

        /**
         * Check for unexpected tokens.
         */
        if (cmd.getArgs().length > 1) {
            String error = "Found unexpected tokens in command line: ";
            for (String unexpected : cmd.getArgs()) {
                error += "\"" + unexpected + "\" ";
            }
            throw new ParseException(error);
        }

        /**
         * Assure that exactly one mode of operation is given.
         */
        int numberOfArgs = 0;

        if (cmd.hasOption("n")) { numberOfArgs++; modeOfOperation = ModeOfOperation.CREATE_ACCOUNT; }
        if (cmd.hasOption("d")) { numberOfArgs++; modeOfOperation = ModeOfOperation.DEPOSIT; }
        if (cmd.hasOption("w")) { numberOfArgs++; modeOfOperation = ModeOfOperation.WITHDRAW; }
        if (cmd.hasOption("g")) { numberOfArgs++; modeOfOperation = ModeOfOperation.GET_BALANCE; }

        if (numberOfArgs != 1) {
            throw new ParseException("Wrong number of  modes of operations: " + numberOfArgs + " given.");
        }

        /**
         * Assure that not argument is longer that 4096 characters.
         * Assure that no option was set more than once.
         */
        HashSet<String> alreadyParsedOptions = new HashSet<>(10);
        for (Option o : cmd.getOptions()) {
            if (o.hasArg()) {
                if (o.getValue().length() > CLIValidator.CHARACTER_LIMIT) {
                    throw new ParseException("The argument for \"" + o.getOpt() + "\" exceeds the " + CLIValidator.CHARACTER_LIMIT + " character limit.");
                }
            }
            if (alreadyParsedOptions.contains(o.getOpt())) {
                throw new ParseException("The argument for \"" + o.getOpt() + "\" was set multiple times.");
            } else {
                alreadyParsedOptions.add(o.getOpt());
            }
        }

        /**
         * Check mode of operation.
         */
        switch (modeOfOperation) {
            case CREATE_ACCOUNT:
                String b = cmd.getOptionValue(ModeOfOperation.CREATE_ACCOUNT.toString());
                CLIValidator.isValidCurrency(b, ModeOfOperation.CREATE_ACCOUNT.toString());
                double balance = Double.parseDouble(b);
                if (balance < 10.00) {
                    throw new ParseException("Argument for \"n\" must be greater equal 10.");
                }
                mooValue = balance;
                break;
            case DEPOSIT:
                String d = cmd.getOptionValue(ModeOfOperation.DEPOSIT.toString());
                CLIValidator.isValidCurrency(d, ModeOfOperation.DEPOSIT.toString());
                double amount = Double.parseDouble(d);
                if (amount < 0.00) {
                    throw new ParseException("Argument for \"d\" must be greater equal 0.");
                }
                mooValue = amount;
                break;
            case WITHDRAW:
                String w = cmd.getOptionValue(ModeOfOperation.WITHDRAW.toString());
                CLIValidator.isValidCurrency(w, ModeOfOperation.WITHDRAW.toString());
                double amount2 = Double.parseDouble(w);
                if (amount2 < 0.00) {
                    throw new ParseException("Argument for \"w\" must be greater equal 0.");
                }
                mooValue = amount2;
                break;
            case GET_BALANCE:
                mooValue = 0.00;
        }


        /**
         * Assure that -a argument (account name) has correct format.
         */
        String account = cmd.getOptionValue("a");
        CLIValidator.isValidAccountname(account, "a");
        this.account = account;

        /**
         * Assure that port is a correct number.
         */
        if (cmd.hasOption("p")) {
            String p = cmd.getOptionValue("p");
            CLIValidator.isValidPort(p, "p");
            int port = Integer.parseInt(p);
            if (1024 > port || port > 65535){
                throw new ParseException("Port number out of range for argument \"-p\".");
            }
            this.port = port;
        } else {
            // Set port to default value (3000).
            this.port = 3000;
        }

        /**
         * Assure that arguments of d, n and w are correct currency numbers.
         */
        if (cmd.hasOption("d")) CLIValidator.isValidCurrency(cmd.getOptionValue("d"), "d");
        if (cmd.hasOption("n")) CLIValidator.isValidCurrency(cmd.getOptionValue("n"), "n");
        if (cmd.hasOption("w")) CLIValidator.isValidCurrency(cmd.getOptionValue("w"), "w");

        /**
         * Assure that -s (auth-file) and -c (card-file) arguments are valid filenames.
         */
        if (cmd.hasOption("s")) {
            String authFile = cmd.getOptionValue("s");
            CLIValidator.isValidFilename(authFile, "s");
            this.authFile = authFile;
        } else {
            // Set aut file to default (bank.auth).
            this.authFile = "bank.auth";
        }

        if (cmd.hasOption("c")) {
            String cardFile = cmd.getOptionValue("c");
            CLIValidator.isValidFilename(cardFile, "c");
            this.cardFile = cardFile;
        } else {
            // Set card name to default (<account>.card).
            this.cardFile = this.account + ".card";
        }

        /**
         * Assure that -i argument (IP address) has correct format.
         */
        if (cmd.hasOption("i")) {
            String ipAddress = cmd.getOptionValue("i");
            CLIValidator.isValidIPAddress(ipAddress, "i");
            this.ipAddress = ipAddress;
        } else {
            // Set IP Address to default (127.0.0.1).
            this.ipAddress = "127.0.0.1";
        }
    }

    public String getAccount() {
        return account;
    }

    public String getAuthFile() {
        return authFile;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getCardFile() {
        return cardFile;
    }

    public ModeOfOperation getModeOfOperation() {
        return modeOfOperation;
    }

    public double getMooValue() {
        return mooValue;
    }

}
