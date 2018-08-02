package de.upb.bibifi.verybest.common.models;

import java.math.BigInteger;
import java.util.Objects;

public class DepositAction extends Action {
    private final String name;
    private final BigInteger amount;

    public DepositAction(String name, BigInteger amount) {
        this.name = name;
        this.amount = amount;
    }

    public String name() {
        return name;
    }

    public BigInteger amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositAction that = (DepositAction) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, amount);
    }

    @Override
    public String toString() {
        return "DepositAction{" +
                "name='" + name + '\'' +
                ", amount=" + amount +
                '}';
    }
}
