package de.upb.bibifi2018.kaffeeklatsch.bank;

import de.upb.bibifi2018.kaffeeklatsch.commands.SuccessResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import okio.ByteString;

public class Account {
  private final String accountId;
  private final ByteString publicKey;
  private BigDecimal balance;

  public Account(ByteString publicKey, String accountId, BigDecimal balance) {
    this.publicKey = publicKey;
    this.accountId = accountId;
    this.balance = balance.setScale(2, RoundingMode.UNNECESSARY);
  }

  public SuccessResult withdraw(BigDecimal amount) {
    if (amount.signum() != 1) {
      throw new IllegalArgumentException("withdraw amount must be greater than 0.00");
    }
    BigDecimal newBalance = this.balance.subtract(amount);
    if (newBalance.signum() < 0) {
      return new SuccessResult(false);
    }
    this.balance = newBalance;
    return new SuccessResult(true);
  }

  public SuccessResult deposit(BigDecimal amount) {
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("deposit amount cannot be negative");
    }
    this.balance = this.balance.add(amount);
    return new SuccessResult(true);
  }

  public String getAccountId() {
    return accountId;
  }

  public BigDecimal getBalance() {
    return this.balance;
  }

  public ByteString getPublicKey() {
    return this.publicKey;
  }
}
