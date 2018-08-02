package de.upb.cs.bibifi.atmapp.util;

import de.upb.cs.bibifi.atmapp.atm.impl.Atm;
import de.upb.cs.bibifi.atmapp.data.AtmInput;
import de.upb.cs.bibifi.commons.constants.SharedConstants;
import de.upb.cs.bibifi.commons.data.AuthFile;
import de.upb.cs.bibifi.commons.dto.TransmissionPacket;
import de.upb.cs.bibifi.commons.impl.EncryptionImpl;
import de.upb.cs.bibifi.commons.validator.Validator;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class CommandLineHandler {

    private CommandLine commandLine;

    private final String[] args;

    private TransmissionPacket packet;

    public CommandLineHandler(String[] args) {
        this.args = args;
        if (Arrays.stream(args).anyMatch(x -> x == null || x.isEmpty()))
            Validator.fail();

        init();

        if (!Validator.validateArgumentLength(this.args))
            Validator.fail();
    }

    private String cardFileName;

    private String ip;

    private Integer port;

    /**
     * Initialize Atm and validate parameters
     */
    private void init() {

        CommandLineParser commandLineParser = new DefaultParser();

        Options options = new Options();

        Option required = new Option(SharedConstants.CMD_A, "account", true, "initial account");
        required.setRequired(true);
        options.addOption(required);
        options.addOption(SharedConstants.CMD_S, "auth", true, "authentication file");
        options.addOption(SharedConstants.CMD_C, "card", true, "user's bank card");
        options.addOption(SharedConstants.CMD_D, "deposit", true, "deposit amount");
        options.addOption(SharedConstants.CMD_W, "withdrawal", true, "withdrawal amount");
        options.addOption(SharedConstants.CMD_I, "ip", true, "initial ip");
        options.addOption(SharedConstants.CMD_P, "port", true, "initial port");
        options.addOption(SharedConstants.CMD_N, "initial", true, "initial balance");
        Option checkBalanceOption = Option.builder(SharedConstants.CMD_G)
                .required(false)
                .hasArg(false)
                .build();
        options.addOption(checkBalanceOption);

        try {

            commandLine = commandLineParser.parse(options, args);

            //validate all commandline parameters
            Validator.applyValidators(commandLine.getOptions());

            // Retrieve the AuthFile Content
            String key = AuthFile.getAuthFile(commandLine.getOptionValue(SharedConstants.CMD_S, "bank.auth")).getKey();

            EncryptionImpl.initialize(key);

            //ip, port and their default values
            this.ip = commandLine.getOptionValue("ip", "127.0.0.1");
            this.port = Integer.parseInt(commandLine.getOptionValue("port", "3000"));

        } catch (UnrecognizedOptionException ex) {
            System.exit(255);
            System.err.println(255);
        } catch (MissingArgumentException e) {
            System.exit(255);
            System.err.println(255);
        } catch (ParseException e) {
            System.exit(255);
            System.err.println(255);
        }
    }

    private String readCardFile() {
        String pin = null;
        try {
            pin = EncryptionImpl.getInstance().decryptMessage(FileUtils.readFileToString(new File(getCardFileName()), "UTF-8"));
        } catch (Exception e) {
            System.err.println(255);
            System.exit(255);
        }
        return pin;
    }


    /**
     * Call businesslogic methods based on input parameters
     */
    private void setTransmissionPacket(TransmissionPacket packet) {
        this.packet = packet;
    }

    public TransmissionPacket getPacket() {
        return packet;
    }

    public Integer getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public CommandLineHandler processCommandLineArguments() {
        Set<String> opts = new HashSet<>();

        Arrays.stream(commandLine.getOptions()).forEach(option -> {
            opts.add(option.getOpt());
        });
        if (!Validator.checkOperations(opts)) {
            Validator.fail();
        }
        this.cardFileName = commandLine.getOptionValue(SharedConstants.CMD_C);
        Arrays.stream(commandLine.getOptions()).forEach(option -> {
            AtmInput input;
            switch (option.getOpt()) {
                case SharedConstants.CMD_N:
                    if (Validator.checkCardFile(getCardFileName())) {
                        Validator.fail();
                    }
                    input = new AtmInput(commandLine, null);
                    setTransmissionPacket(Atm.createAccount(input));
                    break;
                case SharedConstants.CMD_D:
                    input = new AtmInput(commandLine, readCardFile());
                    setTransmissionPacket(Atm.deposit(input));
                    break;
                case SharedConstants.CMD_W:
                    input = new AtmInput(commandLine, readCardFile());
                    setTransmissionPacket(Atm.withdraw(input));
                    break;
                case SharedConstants.CMD_G:
                    input = new AtmInput(commandLine, readCardFile());
                    setTransmissionPacket(Atm.checkBalance(input));
                    break;
            }
        });
        return this;
    }

    public String getCardFileName() {
        if (cardFileName == null)
            cardFileName = commandLine.getOptionValue(SharedConstants.CMD_A) + ".card";
        return cardFileName;
    }
}
