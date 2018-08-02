package de.upb.bibifi.verybest.common.models;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class AccountCreateAction extends Action {

    private final byte[] publicKey;
    private final String name;
    private final BigInteger initialBalance;

    public AccountCreateAction(byte[] publicKey, String name, BigInteger initialBalance) {
        this.publicKey = publicKey;
        this.name = name;
        this.initialBalance = initialBalance;
    }

    public byte[] publicKey() {
        return publicKey;
    }

    public String name() {
        return name;
    }

    public BigInteger initialBalance() {
        return initialBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountCreateAction that = (AccountCreateAction) o;
        return Arrays.equals(publicKey, that.publicKey) &&
                Objects.equals(name, that.name) &&
                Objects.equals(initialBalance, that.initialBalance);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, initialBalance);
        result = 31 * result + Arrays.hashCode(publicKey);
        return result;
    }

    @Override
    public String toString() {
        return "AccountCreateAction{" +
                "publicKey=" + Arrays.toString(publicKey) +
                ", name='" + name + '\'' +
                ", initialBalance=" + initialBalance +
                '}';
    }
}
