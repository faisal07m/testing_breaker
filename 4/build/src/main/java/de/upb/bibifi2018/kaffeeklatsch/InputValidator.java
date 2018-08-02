package de.upb.bibifi2018.kaffeeklatsch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {

  private static final BigDecimal MIN_CURRENCY = BigDecimal.ZERO;
  private static final BigDecimal MAX_CURRENCY =
      new BigDecimal("4294967295.99").setScale(2, RoundingMode.UNNECESSARY);

  public static boolean isNumber(String input) {
    Pattern p = Pattern.compile("0|([1-9][0-9]*)");
    Matcher m = p.matcher(input);

    return m.matches();
  }

  private static final Pattern CURRENCY = Pattern.compile("0\\.[0-9]{2}|[1-9][0-9]*\\.[0-9]{2}");

  public static boolean isCurrency(String input) {

    boolean matches = CURRENCY.matcher(input).matches();
    if (matches) {
      BigDecimal currency = new BigDecimal(input).setScale(2, RoundingMode.UNNECESSARY);

      return currency.compareTo(MIN_CURRENCY) >= 0
          && currency.compareTo(MAX_CURRENCY) <= 0;
    }
    return false;
  }

  public static boolean isBalance(String input) {
    boolean matches = CURRENCY.matcher(input).matches();

    if (matches) {
      BigDecimal d = new BigDecimal(input);
      return d.signum() >= 0;
    }

    return false;
  }

  private static final Pattern FILENAME = Pattern.compile("[_\\-.0-9a-z]{1,127}");

  public static boolean isFileName(String input) {
    if (input.isEmpty() || input.equals(".") || input.equals("..")) {
      return false;
    }

    return FILENAME.matcher(input).matches();
  }

  public static boolean isAccountName(String input) {
    return Pattern.matches("[_\\-.0-9a-z]{1,122}", input);
  }

  public static boolean isIp(String input) {
    return Pattern.matches("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
        + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)", input);
  }

  public static boolean isPort(String input) {
    if (isNumber(input) && input.length() <= 6) {
      int number = Integer.parseInt(input);

      return 1024 <= number && number <= 65535;
    }

    return false;
  }
}
