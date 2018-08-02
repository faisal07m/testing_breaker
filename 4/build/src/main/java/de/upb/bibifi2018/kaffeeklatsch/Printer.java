package de.upb.bibifi2018.kaffeeklatsch;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

public class Printer {

  // Set System.out to System.err
  public static void setStdErr() {
    PrintStream p = System.err;
    System.setOut(p);
  }


  // System.out to be the original System.out
  public static void setStdOut() {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
  }


  // Should be printed if the ATM throws ProtocolFailureException or the Bank timeouts
  public static void printProtocolError() {
    System.out.print("protocol_error" + "\n");
    System.out.flush();
  }

  // Server has been started
  public static void printServerCreated() {
    System.out.print("created" + "\n");
    System.out.flush();
  }

  // Create account
  public static void printCreateAccount(final String account,
      final BigDecimal balance) {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive(account));
    jsonObject.add(JsonConstants.INITIAL_BALANCE_KEY,
        new JsonPrimitive(balance));

    if (InputValidator.isCurrency(jsonObject
        .get(JsonConstants.INITIAL_BALANCE_KEY).getAsString())) {
      if (InputValidator.isAccountName(jsonObject
          .get(JsonConstants.ACCOUNT_KEY).getAsString())) {
        System.out.print(jsonObject.toString() + "\n");
        System.out.flush();
      } else {
        System.err.print("Printer.printCreateAccount: wrong account name "
            + jsonObject.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n");
      }

    } else {
      System.err.print("Printer.printCreateAccount: wrong currency "
          + jsonObject.get(JsonConstants.INITIAL_BALANCE_KEY)
          .getAsString() + "\n");
    }
  }

  // Deposit
  public static void printDeposit(final String account,
      final BigDecimal depositAmount) {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.add(JsonConstants.ACCOUNT_KEY,
        new JsonPrimitive(account));
    jsonObject.add(JsonConstants.DEPOSIT_KEY,
        new JsonPrimitive(depositAmount));

    if (InputValidator.isCurrency(jsonObject
        .get(JsonConstants.DEPOSIT_KEY).getAsString())) {
      if (InputValidator.isAccountName(jsonObject
          .get(JsonConstants.ACCOUNT_KEY).getAsString())) {
        System.out.print(jsonObject.toString() + "\n");
        System.out.flush();
      } else {
        System.err.print("Printer.printDeposit: wrong account name "
            + jsonObject.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n");
      }

    } else {
      System.err.print("Printer.printDeposit: wrong currency "
          + jsonObject.get(JsonConstants.DEPOSIT_KEY).getAsString()
          + "\n");
    }
  }

  // Withdraw
  public static void printWithdraw(final String account,
      final BigDecimal withdrawAmount) {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive(account));
    jsonObject.add(JsonConstants.WITHDRAW_KEY,
        new JsonPrimitive(withdrawAmount));

    if (InputValidator.isCurrency(jsonObject
        .get(JsonConstants.WITHDRAW_KEY).getAsString())) {
      if (InputValidator.isAccountName(jsonObject
          .get(JsonConstants.ACCOUNT_KEY).getAsString())) {
        System.out.print(jsonObject.toString() + "\n");
        System.out.flush();
      } else {
        System.err.print("Printer.printWithdraw: wrong account name "
            + jsonObject.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n");
      }
    } else {
      System.err.print("Printer.printWithdraw: wrong currency "
          + jsonObject.get(JsonConstants.WITHDRAW_KEY).getAsString() + "\n");
    }
  }

  // Balance
  public static void printBalance(final String account,
      final BigDecimal balance) {
    final JsonObject jsonObject = new JsonObject();
    jsonObject.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive(account));
    jsonObject.add(JsonConstants.BALANCE_KEY, new JsonPrimitive(balance));

    if (InputValidator.isBalance(jsonObject
        .get(JsonConstants.BALANCE_KEY).getAsString())) {
      if (InputValidator.isAccountName(jsonObject
          .get(JsonConstants.ACCOUNT_KEY).getAsString())) {
        System.out.print(jsonObject.toString() + "\n");
        System.out.flush();
      } else {
        System.err.print("Printer.printBalance: wrong account name "
            + jsonObject.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n");
      }
    } else {
      System.err.print("Printer.printBalance: wrong balance "
          + jsonObject.get(JsonConstants.BALANCE_KEY).getAsString() + "\n");
    }
  }
}
