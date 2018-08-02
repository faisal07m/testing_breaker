package de.upb.bibifi.verybest.common.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class InputValidationUtil {

    public static boolean isValidFilename(String filename) {
        if (isInvalidFilename(filename)) {
            return false;
        }
        return filename.chars().parallel().allMatch(c -> isCharValidForFilename((char) c));
    }

    public static boolean isValidAccountName(String account) {
        if (StringUtils.isAllBlank(account) || account.length() > 122) {
            return false;
        }
        return account.chars().parallel().allMatch(c -> isCharValidForFilename((char) c));
    }

    public static boolean isCharValidForFilename(char c) {
        return c == '.' || c == '-' || c == '_' || CharUtils.isAsciiNumeric(c) || CharUtils.isAsciiAlphaLower(c);
    }

    private static boolean isInvalidFilename(String filename) {
        return StringUtils.isAllBlank(filename) || filename.equals(".") || filename.equals("..") || filename.length() > MAX_FILENAME_LENGTH;
    }

    public static final int MIN_VALID_PORT = 1024;
    public static final int MAX_VALID_PORT = 65535;
    public static final int MAX_ARG_LENGTH = 4096;
    public static final int MAX_FILENAME_LENGTH = 127;

    private static final BigDecimal MIN_VALID_NUMBER = BigDecimal.valueOf(0);
    private static final BigDecimal MAX_VALID_NUMBER = BigDecimal.valueOf(4294967295.99);

    public static boolean isPortValid(int port) {
        return port >= MIN_VALID_PORT && port <= MAX_VALID_PORT;
    }

    public static boolean isValidNumber(BigDecimal number) {
        int numberOfDecimalPlaces = numberOfDecimalPlaces(number);
        return numberOfDecimalPlaces <= 2 && MIN_VALID_NUMBER.compareTo(number) <= 0 && number.compareTo(MAX_VALID_NUMBER) <= 0;
    }

    private static int numberOfDecimalPlaces(BigDecimal bigDecimal) {
        String string = bigDecimal.stripTrailingZeros().toPlainString();
        int index = string.indexOf(".");
        return index < 0 ? 0 : string.length() - index - 1;
    }

    public static final Pattern VALID_IP_FORMAT = Pattern.compile("^(25[0-5]|2[0-4]\\d|1?[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1?[1-9]?\\d)){3}$");
    public static final Pattern VALID_PORT_FORMAT = Pattern.compile("^[1-9][0-9]*$");
    public static final Pattern VALID_NUMBER_FORMAT = Pattern.compile("^(0|[1-9][0-9]*).([0-9][0-9])$");
}
