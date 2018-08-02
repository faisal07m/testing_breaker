package de.upb.bionicbeaver.atm.validation;

import org.apache.commons.cli.*;

import java.util.regex.Pattern;

public class CLIValidator {

    public final static int CHARACTER_LIMIT = 4096;

    public static final boolean isValidCurrency(String c, String option) throws ParseException {
        Pattern currencyPattern = Pattern.compile("(0|([1-9][0-9]*))\\.[0-9]{2}");
        if (!currencyPattern.matcher(c).matches()) {
            throw new ParseException("Invalid currency number format for argument \"" + option + "\".");
        } else if (Double.parseDouble(c) <= 0.00d || Double.parseDouble(c) > 4294967295.99d) {
            throw new ParseException("Currency argument for argument \"" + option + "\" not in range [0, 4294967295.99].");
        }
        return true;
    }

    public static final boolean isValidPort(String n, String option) throws ParseException {
        Pattern numPattern = Pattern.compile("([1-9][0-9]*)");
        if (!numPattern.matcher(n).matches()) {
            throw new ParseException("Invalid number format for argument \"" + option + "\".");
        }
        return true;
    }

    public static final boolean  isValidFilename(String f, String option) throws ParseException {
        Pattern filePattern = Pattern.compile("[_\\-\\.0-9a-z]{1,127}");
        if (!filePattern.matcher(f).matches() || f.equals(".") || f.equals("..")) {
            throw new ParseException("Invalid file name format for argument \"" + option + "\".");
        }
        return true;
    }

    public static final boolean isValidAccountname(String f, String option) throws ParseException {
        Pattern filePattern = Pattern.compile("[_\\-\\.0-9a-z]{1,122}");
        if (!filePattern.matcher(f).matches()) {
            throw new ParseException("Invalid account name format for argument \"" + option + "\".");
        }
        return true;
    }

    public static final boolean isValidIPAddress(String ip, String option) throws ParseException {
        if(ip.startsWith("0"))
            throw new ParseException("Invalid IP address format for argument \"" + option + "\".");

        Pattern ipPattern = Pattern.compile("([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))");
        if (!ipPattern.matcher(ip).matches()) {
            throw new ParseException("Invalid IP address format for argument \"" + option + "\".");
        }
        return true;
    }



}
