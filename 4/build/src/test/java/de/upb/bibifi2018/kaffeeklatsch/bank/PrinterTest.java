package de.upb.bibifi2018.kaffeeklatsch.bank;


import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.upb.bibifi2018.kaffeeklatsch.Printer;
import de.upb.bibifi2018.kaffeeklatsch.commands.JsonConstants;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrinterTest {

  ByteArrayOutputStream sOut = new ByteArrayOutputStream();
  ByteArrayOutputStream sErr = new ByteArrayOutputStream();

  @Test
  public void testPrintCreateAccount() {
    System.out.flush();
    System.err.flush();

    sOut.reset();
    sErr.reset();

    System.setOut(new PrintStream(sOut));
    System.setErr(new PrintStream(sErr));

    BigDecimal d = new BigDecimal("123.00");
    JsonObject jCA1 = new JsonObject();
    jCA1.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jCA1.add(JsonConstants.INITIAL_BALANCE_KEY, new JsonPrimitive(d));

    Printer.printCreateAccount("hans", d);
    assertEquals(jCA1.toString() + "\n", sOut.toString());
    assertEquals("", sErr.toString());

    sOut.reset();
    sErr.reset();

    JsonObject jCA2 = new JsonObject();
    jCA2.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("WRONG NAME"));
    jCA2.add(JsonConstants.INITIAL_BALANCE_KEY, new JsonPrimitive(d));

    Printer.printCreateAccount("WRONG NAME", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printCreateAccount: wrong account name "
        + jCA2.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();

    d = new BigDecimal("-123.00");
    JsonObject jCA3 = new JsonObject();
    jCA3.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("goodname"));
    jCA3.add(JsonConstants.INITIAL_BALANCE_KEY, new JsonPrimitive(d));

    Printer.printCreateAccount("goodname", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printCreateAccount: wrong currency "
        + jCA3.get(JsonConstants.INITIAL_BALANCE_KEY).getAsString()
        + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();

    d = new BigDecimal("1.044");
    JsonObject jCA4 = new JsonObject();
    jCA4.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("name"));
    jCA4.add(JsonConstants.INITIAL_BALANCE_KEY, new JsonPrimitive(d));

    Printer.printCreateAccount("name", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printCreateAccount: wrong currency "
        + jCA4.get(JsonConstants.INITIAL_BALANCE_KEY).getAsString()
        + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    System.out.flush();
    System.err.flush();

    Printer.setStdOut();
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
  }

  @Test
  public void testPrintDeposit() {
    System.out.flush();
    System.err.flush();

    sOut.reset();
    sErr.reset();

    System.setOut(new PrintStream(sOut));
    System.setErr(new PrintStream(sErr));


    BigDecimal d = new BigDecimal("0.00");
    JsonObject jD1 = new JsonObject();
    jD1.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jD1.add(JsonConstants.DEPOSIT_KEY, new JsonPrimitive(d));

    Printer.printDeposit("hans", d);
    assertEquals(jD1.toString() + "\n", sOut.toString());
    assertEquals("", sErr.toString());

    sOut.reset();
    sErr.reset();


    JsonObject jD2 = new JsonObject();
    jD2.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("BaDNamE"));
    jD2.add(JsonConstants.DEPOSIT_KEY, new JsonPrimitive(d));

    Printer.printDeposit("BaDNamE", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printDeposit: wrong account name "
        + jD2.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    d = new BigDecimal("-123.00");
    JsonObject jD3 = new JsonObject();
    jD3.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jD3.add(JsonConstants.DEPOSIT_KEY, new JsonPrimitive(d));

    Printer.printDeposit("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printDeposit: wrong currency "
        + jD3.get(JsonConstants.DEPOSIT_KEY).getAsString()
        + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    d = new BigDecimal( "1.1234");
    JsonObject jD4 = new JsonObject();
    jD4.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jD4.add(JsonConstants.DEPOSIT_KEY, new JsonPrimitive(d));

    Printer.printDeposit("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printDeposit: wrong currency "
        + jD4.get(JsonConstants.DEPOSIT_KEY).getAsString()
        + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    System.out.flush();
    System.err.flush();

    Printer.setStdOut();
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
  }

  @Test
  public void testPrintWithdraw() {
    System.out.flush();
    System.err.flush();

    sOut.reset();
    sErr.reset();

    System.setOut(new PrintStream(sOut));
    System.setErr(new PrintStream(sErr));

    BigDecimal d = new BigDecimal("123.45");
    JsonObject jW1 = new JsonObject();
    jW1.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jW1.add(JsonConstants.WITHDRAW_KEY, new JsonPrimitive(d));

    Printer.printWithdraw("hans", d);
    assertEquals(jW1.toString() + "\n", sOut.toString());
    assertEquals("", sErr.toString());

    sOut.reset();
    sErr.reset();


    JsonObject jW2 = new JsonObject();
    jW2.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("name?"));
    jW2.add(JsonConstants.WITHDRAW_KEY, new JsonPrimitive(d));

    Printer.printWithdraw("name?", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printWithdraw: wrong account name "
        + jW2.get(JsonConstants.ACCOUNT_KEY).getAsString()
        + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();

    d = new BigDecimal("-1.00");
    JsonObject jW3 = new JsonObject();
    jW3.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jW3.add(JsonConstants.WITHDRAW_KEY, new JsonPrimitive(d));

    Printer.printWithdraw("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printWithdraw: wrong currency "
        + jW3.get(JsonConstants.WITHDRAW_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();

    d = new BigDecimal("1.23456789");
    JsonObject jW4 = new JsonObject();
    jW4.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jW4.add(JsonConstants.WITHDRAW_KEY, new JsonPrimitive(d));

    Printer.printWithdraw("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printWithdraw: wrong currency "
        + jW4.get(JsonConstants.WITHDRAW_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    System.out.flush();
    System.err.flush();

    Printer.setStdOut();
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
  }

  @Test
  public void testPrintBalance() {
    System.out.flush();
    System.err.flush();

    sOut.reset();
    sErr.reset();

    System.setOut(new PrintStream(sOut));
    System.setErr(new PrintStream(sErr));

    String bigNumber = "";
    for (int i = 0; i < 1000; i++) {
      bigNumber += "9999999999999999999999999999999999999999999999999";
    }

    bigNumber += ".99";

    BigDecimal d = new BigDecimal(bigNumber);
    JsonObject jB1 = new JsonObject();
    jB1.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jB1.add(JsonConstants.BALANCE_KEY, new JsonPrimitive(d));

    Printer.printBalance("hans", d);
    assertEquals(jB1.toString() + "\n", sOut.toString());
    assertEquals("", sErr.toString());

    sOut.reset();
    sErr.reset();


    JsonObject jB2 = new JsonObject();
    jB2.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("!?"));
    jB2.add(JsonConstants.BALANCE_KEY, new JsonPrimitive(d));

    Printer.printBalance("!?", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printBalance: wrong account name "
        + jB2.get(JsonConstants.ACCOUNT_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    d = new BigDecimal("-4.00");
    JsonObject jB3 = new JsonObject();
    jB3.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jB3.add(JsonConstants.BALANCE_KEY, new JsonPrimitive(d));

    Printer.printBalance("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printBalance: wrong balance "
        + jB3.get(JsonConstants.BALANCE_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    d = new BigDecimal("4.567890123456789");
    JsonObject jB4 = new JsonObject();
    jB4.add(JsonConstants.ACCOUNT_KEY, new JsonPrimitive("hans"));
    jB4.add(JsonConstants.BALANCE_KEY, new JsonPrimitive(d));

    Printer.printBalance("hans", d);
    assertEquals("", sOut.toString());
    assertEquals("Printer.printBalance: wrong balance "
        + jB4.get(JsonConstants.BALANCE_KEY).getAsString() + "\n", sErr.toString());

    sOut.reset();
    sErr.reset();


    System.out.flush();
    System.err.flush();

    Printer.setStdOut();
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
  }
}
