package de.upb.bibifi2018.kaffeeklatsch.atm;

import com.codahale.xsalsa20poly1305.SimpleBox;
import de.upb.bibifi2018.kaffeeklatsch.AuthFile;
import de.upb.bibifi2018.kaffeeklatsch.AuthKeys;
import de.upb.bibifi2018.kaffeeklatsch.CardFile;
import de.upb.bibifi2018.kaffeeklatsch.ExitCode;
import de.upb.bibifi2018.kaffeeklatsch.InputValidator;
import de.upb.bibifi2018.kaffeeklatsch.Printer;
import de.upb.bibifi2018.kaffeeklatsch.StrictArgParser;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.BankingFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.CardFileAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InsuficcientFundsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidAmountException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.InvalidCardFileException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ProtocolFailureException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.ReplayAttackException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import okio.ByteString;

public class AtmMain {

  private final String ipAddress;
  private final int port;
  private final AuthFile authFile;
  private final String cardFileName;
  private final String account;
  private final Action action;
  private final BigDecimal amount;

  private AuthKeys authKeys;
  private CardFile cardFile;

  public AtmMain(String ipAddress, int port, AuthFile authFile, String cardFileName,
                 String account, Action action, BigDecimal amount) {


    this.ipAddress = ipAddress;
    this.port = port;
    this.authFile = authFile;
    this.cardFileName = cardFileName;
    this.account = account;
    this.action = action;
    this.amount = amount;
  }

  private enum Action {
    WITHDRAW,
    DEPOSIT,
    CREATE,
    BALANCE
  }

  public static void main(String[] args) {
    startTimeout();
    AtmMain atmMain;
    try {
      StrictArgParser argParser = new StrictArgParser(args, "g", "ipscandw");
      argParser.hasExactlyOneOf("ndwg");
      argParser.hasAll("a");


      boolean valid = true;
      String ip = argParser.getParameter('i', "127.0.0.1");
      valid = valid && InputValidator.isIp(ip);

      String portStr = argParser.getParameter('p', "3000");
      valid = valid && InputValidator.isPort(portStr);

      String authFileName = argParser.getParameter('s', "bank.auth");
      valid = valid && InputValidator.isFileName(authFileName);

      String accountName = argParser.getParameter('a');
      valid = valid && InputValidator.isAccountName(accountName);

      String cardFileName = argParser.getParameter('c', accountName + ".card");
      valid = valid && InputValidator.isFileName(cardFileName);

      if (!valid) {
        throw new Exception();
      }

      AuthFile authFile = new AuthFile(authFileName);

      Action action;
      BigDecimal amount;

      if (argParser.getFlag('g')) {

        action = Action.BALANCE;
        amount = null;

      } else {

        String amountStr;
        if (argParser.hasParameter('d')) {
          action = Action.DEPOSIT;
          amountStr = argParser.getParameter('d');
        } else if (argParser.hasParameter('w')) {
          action = Action.WITHDRAW;
          amountStr = argParser.getParameter('w');
        } else if (argParser.hasParameter('n')) {
          action = Action.CREATE;
          amountStr = argParser.getParameter('n');
        } else {
          // should not happen, since argParser.hasExactlyOneOf("ndwg")
          throw new Exception("missing action");
        }

        if (!InputValidator.isCurrency(amountStr)) {
          throw new Exception();
        }
        amount = new BigDecimal(amountStr);

      }

      int port = Integer.parseInt(portStr);

      atmMain = new AtmMain(ip, port, authFile, cardFileName, accountName, action, amount);
      try {
        atmMain.run();
      } catch (Throwable t) {
        // ssshh now, quiet
        Runtime.getRuntime().halt(ExitCode.UNKNOWN_FAILURE.getExitCode());
      }
    } catch (Throwable e) {
      Runtime.getRuntime().halt(ExitCode.ARGUMENT_VIOLATION.getExitCode());
    }


  }

  protected void exit(ExitCode exitCode) {
    System.exit(exitCode.getExitCode());
  }

  private static void startTimeout() {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    Thread timeoutThread = new Thread(() -> {
      while (System.nanoTime() < deadline) {
        try {
          Thread.sleep(300);
        } catch (InterruptedException e) {
          // ignore
        }
      }
      System.exit(ExitCode.TIMEOUT.getExitCode());
    }, "TimeoutThread");

    timeoutThread.setDaemon(true);
    timeoutThread.setPriority(Thread.MAX_PRIORITY);
    timeoutThread.start();
  }

  /**
   * Run the ATM.
   */
  protected void run() {

    try {
      this.authKeys = this.authFile.readPrivateKey();
    } catch (IOException e) {
      System.err.println("file invalid");
      exit(ExitCode.ARGUMENT_VIOLATION);
    }

    this.cardFile = new CardFile(this.authKeys.asSimpleBox(), this.cardFileName);

    AtmClient client = null;
    ExitCode exitCode = ExitCode.ARGUMENT_VIOLATION;
    try {
      client = new AtmClient(this.authKeys, this.ipAddress, this.port, this.account);
      if (this.action == Action.CREATE) {
        if (this.cardFile.exists()) {
          throw new CardFileAlreadyExistsException();
        }

        this.createAccount(client, amount);
      } else {
        ByteString cardPrivateKey = this.cardFile.readPrivateKey(this.account);
        SimpleBox card = new SimpleBox(this.authKeys.getBankPublicKey(), cardPrivateKey);

        if (this.action == Action.WITHDRAW) {
          this.withdraw(client, card, amount);
        } else if (this.action == Action.DEPOSIT) {
          this.deposit(client, card, amount);
        } else if (this.action == Action.BALANCE) {
          this.retrieveBalance(client, card);
        } else {
          throw new IllegalStateException();
        }
      }
      exitCode = ExitCode.SUCCESS;
    } catch (IOException | ProtocolFailureException | ReplayAttackException e) {
      exitCode = ExitCode.PROTOCOL_COMMUNICATION_FAILURE;
    } catch (AccountAlreadyExistsException e) {
      exitCode = ExitCode.DUPLICATE_ACCOUNT;
    } catch (InvalidAmountException
        | NoSuchAccountException
        | InsuficcientFundsException
        | BankingFailureException e) {

      exitCode = ExitCode.BANKING_FAILURE;
    } catch (InvalidCardException | InvalidCardFileException e) {
      exitCode = ExitCode.INVALID_CARD;
    } catch (CardFileAlreadyExistsException e) {
      exitCode = ExitCode.DUPLICATE_CARD_FILE;
    } finally {
      try {
        if (client != null) {
          client.close();
        }
      } finally {
        exit(exitCode);
      }
    }
  }

  private void createAccount(AtmClient client, BigDecimal initialBalance)
      throws AccountAlreadyExistsException, ProtocolFailureException, IOException,
      InvalidAmountException, ReplayAttackException {

    if (initialBalance.subtract(new BigDecimal("10.00")).signum() < 0) {
      throw new InvalidAmountException();
    }

    client.connectWithoutCard();

    final CreateAccountResult result = client.createAccount(amount);
    Printer.printCreateAccount(account, initialBalance);
    this.cardFile.writePrivateKey(result.getPrivateKey(), account);
  }

  private void deposit(AtmClient client, SimpleBox card, BigDecimal depositAmount)
      throws IOException, BankingFailureException, InvalidCardException,
      ProtocolFailureException, NoSuchAccountException,
      InvalidAmountException, ReplayAttackException {

    if (depositAmount.signum() < 1) {
      throw new InvalidAmountException();
    }

    client.connectWithCard(card);

    SuccessResult deposit = client.deposit(depositAmount);
    if (!deposit.isSuccess()) {
      throw new BankingFailureException();
    }
    Printer.printDeposit(account, depositAmount);
  }

  private void withdraw(AtmClient client, SimpleBox card, BigDecimal withdrawAmount) throws
      IOException, InvalidAmountException, InvalidCardException,
      ProtocolFailureException, NoSuchAccountException,
      InsuficcientFundsException, ReplayAttackException {

    if (withdrawAmount.signum() < 1) {
      throw new InvalidAmountException();
    }

    client.connectWithCard(card);

    SuccessResult result = client.withdraw(withdrawAmount);
    if (!result.isSuccess()) {
      throw new InsuficcientFundsException();
    }
    Printer.printWithdraw(account, withdrawAmount);
  }

  private void retrieveBalance(AtmClient client, SimpleBox card)
      throws InvalidCardException, NoSuchAccountException,
      ProtocolFailureException, IOException, ReplayAttackException {

    client.connectWithCard(card);
    BalanceResult balance = client.balance();
    Printer.printBalance(this.account, balance.getBalance());
  }
}
