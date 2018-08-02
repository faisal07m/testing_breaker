package de.upb.bibifi.verybest.common.models;

import com.google.auto.value.AutoValue;

import java.math.BigInteger;

@AutoValue
public abstract class AccountCreateActionTemplate {
    public static AccountCreateActionTemplate create(String name, BigInteger initialBalance) {
        return new AutoValue_AccountCreateActionTemplate(name, initialBalance);
    }

    public abstract String name();

    public abstract BigInteger initialBalance();
}
