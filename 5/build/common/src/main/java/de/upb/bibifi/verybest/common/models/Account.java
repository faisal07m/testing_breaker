package de.upb.bibifi.verybest.common.models;

import com.google.auto.value.AutoValue;

import java.math.BigInteger;

@AutoValue
public abstract class Account {
    public static Account create(String name, byte[] publicKey, BigInteger balance) {
        return new AutoValue_Account.Builder()
                .name(name)
                .publicKey(publicKey)
                .balance(balance)
                .build();
    }

    public abstract String name();

    @SuppressWarnings("mutable")
    public abstract byte[] publicKey();

    public abstract BigInteger balance();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder publicKey(byte[] publicKey);

        public abstract Builder balance(BigInteger balance);

        public abstract Account build();
    }
}
