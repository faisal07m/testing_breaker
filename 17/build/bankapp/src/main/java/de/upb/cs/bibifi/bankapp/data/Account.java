package de.upb.cs.bibifi.bankapp.data;

import java.math.BigDecimal;

public class Account {
    private BigDecimal balance;
    private final String name;
    private final String hashedPin;

    public Account(String balance, String name, String hashedPin) {
        this.balance = new BigDecimal(balance);
        this.name = name;
        this.hashedPin = hashedPin;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public String getName() {
        return name;
    }

    public boolean addBalance(BigDecimal newBalance) {
        this.balance = this.balance.add(newBalance);
        return true;
    }

    public boolean withdrawBalance(BigDecimal balance) {
        if ((this.balance.compareTo(balance) == 0 || this.balance.compareTo(balance) == 1) && balance.compareTo(BigDecimal.ZERO) == 1) {
            this.balance = this.balance.subtract(balance);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;

        Account that = (Account) obj;
        return this.balance.compareTo(that.balance) == 0 && this.hashedPin.equals(that.hashedPin) && this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + balance.hashCode();
        result = 31 * result + hashedPin.hashCode();
        return result;
    }
}
