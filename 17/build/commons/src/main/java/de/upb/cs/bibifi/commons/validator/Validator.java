package de.upb.cs.bibifi.commons.validator;

import de.upb.cs.bibifi.commons.constants.SharedConstants;
import org.apache.commons.cli.Option;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Validator {

    /**
     * check if arguments are duplicated
     *
     * @param option
     * @param options
     * @return
     */
    public static boolean checkDuplicates(String option, Set<String> options) {
        return options.add(option);
    }

    /**
     * Check regex validation for numeric value
     *
     * @param numeral
     * @return
     */
    public static boolean validateNumerals(String numeral) {
        double max_amount = 4294967295.99;
        double amount = 0;
        Pattern pattern = Pattern.compile("^(0|[1-9][0-9]*)\\.\\d{2}$");
        try {
            amount = Double.parseDouble(numeral);
        } catch (NumberFormatException e) {

        }
        return numeral.matches(pattern.pattern()) && amount <= max_amount;
    }

    /**
     * Check regex validation for IP address
     *
     * @param ipString
     * @return
     */
    public static boolean validateIP(String ipString) {
        Pattern pattern = Pattern.compile(
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return pattern.matcher(ipString).matches();
    }

    /**
     * Check regex validation for port
     *
     * @param portString
     * @return
     */
    public static boolean validatePort(String portString) {
        if (!portString.matches("^[1-9][0-9]{3,4}$")) {
            return false;
        }
        int min_num = 1024;
        int max_num = 65535;
        int amount = 0;
        try {
            amount = Integer.parseInt(portString);
        } catch (NumberFormatException e) {

        }
        return amount >= min_num && amount <= max_num;
    }

    /**
     * Check regex validation for File names
     *
     * @param fileName
     * @return
     */
    public static boolean validateFileName(String fileName) {
        Pattern pattern = Pattern.compile("[_\\-\\.0-9a-z]+$");
        return (fileName.length() <= 127 && !fileName.equals(".") && !fileName.equals("..")) && pattern.matcher(fileName).matches();
    }

    /**
     * Check regex validation for File names
     *
     * @param accountName
     * @return
     */
    public static boolean validateAccountName(String accountName) {
        Pattern pattern = Pattern.compile("[_\\-\\.0-9a-z]+$");
        return accountName.length() < 123 && pattern.matcher(accountName).matches();
    }

    /**
     * Check regex validation for File names
     *
     * @param initialBalance
     * @return
     */
    public static boolean validateInitialBalance(String initialBalance) {
        double amount = 0;
        try {
            amount = Double.parseDouble(initialBalance);
        } catch (NumberFormatException e) {

        }
        return amount >= 10.0;
    }

    /**
     * check if arguments contain more than one Operations
     *
     * @param options
     * @return
     */
    public static boolean checkOperations(Set<String> options) {

        final String CMD_D = "d";
        final String CMD_W = "w";
        final String CMD_N = "n";
        final String CMD_G = "g";

        Set<String> operations = new HashSet<>();
        operations.add(CMD_D);
        operations.add(CMD_W);
        operations.add(CMD_N);
        operations.add(CMD_G);

        boolean match = false;
        for (String str : operations) {
            if (options.contains(str) && !match) {
                match = true;
            } else if (options.contains(str) && match) {
                match = false;
                break;
            }
        }
        return match;
    }

    /**
     * check if card file Exists
     *
     * @param Path
     * @return
     */
    public static boolean checkCardFile(String Path) {
        File file = new File(Path);
        return file.exists();
    }

    /**
     * Check regex validation for File names
     *
     * @param args
     * @return
     */
    public static boolean validateArgumentLength(String[] args) {
        return Arrays.stream(args).noneMatch(arg -> arg.length() > 4096);
    }

    /**
     * Apply set of validators to all input parameters
     *
     * @param options
     * @return
     */
    public static void applyValidators(Option[] options) {

        Set<String> duplicateOptionsSet = new HashSet<>();

        Arrays.stream(options).forEach(option -> {

            if (!checkDuplicates(option.getOpt(), duplicateOptionsSet))
                fail();

            switch (option.getOpt()) {
                case SharedConstants.CMD_N:
                    if (!validateNumerals(option.getValue()) || !Validator.validateInitialBalance(option.getValue()))
                        fail();
                    break;
                case SharedConstants.CMD_D:
                case SharedConstants.CMD_W:
                    if (!validateNumerals(option.getValue()))
                        fail();
                    break;
                case SharedConstants.CMD_I:
                    if (!validateIP(option.getValue()))
                        fail();
                    break;
                case SharedConstants.CMD_P:
                    if (!validatePort(option.getValue()))
                        fail();
                    break;
                case SharedConstants.CMD_C:
                case SharedConstants.CMD_S:
                    if (!validateFileName(option.getValue()))
                        fail();
                    break;
                case SharedConstants.CMD_A:
                    if (!validateAccountName((option.getValue())))
                        fail();
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Exit application by displaying en error code.
     */
    public static void fail() {
        System.exit(255);
    }
}
