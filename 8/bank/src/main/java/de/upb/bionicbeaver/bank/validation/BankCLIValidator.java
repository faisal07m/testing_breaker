package de.upb.bionicbeaver.bank.validation;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BankCLIValidator {

    private String authFile;
    private int port;

    public BankCLIValidator(String[] args) {

        /**
         * Specify command line options.
         */
        Option opt_s = new Option("s", true, "Auth file");
        Option opt_p = new Option("p", true, "Port");

        Options posixOptions = new Options();
        posixOptions
                .addOption(opt_s)
                .addOption(opt_p);

        CommandLineParser posixParser = new DefaultParser();

        try {
            CommandLine cmd = posixParser.parse(posixOptions, args);

            /**
             * Assure that not argument is longer that 4096 characters.
             */
            for (Option o : cmd.getOptions()) {
                if (o.hasArg()) {
                    if (o.getValue().length() > CLIValidator.CHARACTER_LIMIT) {
                        throw new ParseException("A argument exceeds the " + CLIValidator.CHARACTER_LIMIT + " character limit.");
                    }
                }
            }

            /**
             * Assure that port is a correct number.
             */
            if (cmd.hasOption("p")) {
                String p = cmd.getOptionValue("p");
                CLIValidator.isValidNumber(p, "p");
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
             * Assure that -s (auth-file)
             */
            if (cmd.hasOption("s")) {
                String authFile = cmd.getOptionValue("s");
                CLIValidator.isValidFilename(authFile, "s");
                this.authFile = authFile;
            } else {
                // Set aut file to default (bank.auth).
                this.authFile = "bank.auth";
            }

        } catch (ParseException e) {
            System.exit(255);
        }
    }

    public String getAuthFileName() {
        return authFile;
    }

    public int getPort() {
        return port;
    }

}
