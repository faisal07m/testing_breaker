package de.upb.bibifi2018.kaffeeklatsch.bank;

import com.codahale.xsalsa20poly1305.Keys;
import de.upb.bibifi2018.kaffeeklatsch.Printer;
import de.upb.bibifi2018.kaffeeklatsch.commands.Balance;
import de.upb.bibifi2018.kaffeeklatsch.commands.BalanceResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccount;
import de.upb.bibifi2018.kaffeeklatsch.commands.CreateAccountResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.Deposit;
import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import de.upb.bibifi2018.kaffeeklatsch.commands.Withdraw;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.AccountAlreadyExistsException;
import de.upb.bibifi2018.kaffeeklatsch.exceptions.NoSuchAccountException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import okio.ByteString;

/**
 * Thread-safe core data-set of the bank.
 */
public class BankReaktor {

  private final Map<String, Account> accounts;

  public BankReaktor() {
    accounts = new HashMap<>();
  }

  public synchronized CreateAccountResult createAccount(CreateAccount command)
      throws AccountAlreadyExistsException {

    if (this.accounts.containsKey(command.getAccountId())) {
      throw new AccountAlreadyExistsException();
    }
    ByteString privateKey = Keys.generatePrivateKey();
    ByteString publicKey = Keys.generatePublicKey(privateKey);
    Account account = new Account(publicKey, command.getAccountId(), command.getInitialBalance());
    this.accounts.put(account.getAccountId(), account);

    Printer.printCreateAccount(command.getAccountId(), command.getInitialBalance());
    return new CreateAccountResult(privateKey);
  }

  /**
   * Since accounts cannot be deleted, if this returns a key,
   * this key is valid while this {@link BankReaktor} instance lives.
   *
   * @throws NoSuchAccountException
   */
  public ByteString getAccountPublicKey(String accountId) throws NoSuchAccountException {
    Account account = this.accounts.get(accountId);
    if (account != null) {
      return account.getPublicKey();
    } else {
      throw new NoSuchAccountException();
    }
  }

  public synchronized SuccessResult withdraw(Withdraw command) throws NoSuchAccountException {
    Account account = this.accounts.get(command.getAccountId());
    if (account == null) {
      throw new NoSuchAccountException();
    }
    SuccessResult successResult = account.withdraw(command.getAmount());
    if (successResult.isSuccess()) {
      Printer.printWithdraw(command.getAccountId(), command.getAmount());
    }
    return successResult;
  }

  public synchronized SuccessResult deposit(Deposit command) throws NoSuchAccountException {
    Account account = this.accounts.get(command.getAccountId());
    if (account == null) {
      throw new NoSuchAccountException();
    }
    SuccessResult successResult = account.deposit(command.getAmount());
    Printer.printDeposit(command.getAccountId(), command.getAmount());
    return successResult;
  }

  public synchronized BalanceResult balance(Balance command) throws NoSuchAccountException {
    Account account = this.accounts.get(command.getAccountId());
    if (account == null) {
      throw new NoSuchAccountException();
    }
    BigDecimal balance = account.getBalance();
    Printer.printBalance(account.getAccountId(), account.getBalance());
    return new BalanceResult(balance);
  }
}
