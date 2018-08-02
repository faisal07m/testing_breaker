package de.upb.bibifi.verybest.common.models;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Objects;

public class AccountAndSession {

    @Nullable
    private final Account account;

    private final BigInteger session;

    private final int errorCode;

    public AccountAndSession(@Nullable Account account, BigInteger session, int errorCode) {
        this.account = account;
        this.session = session;
        this.errorCode = errorCode;
    }

    @Nullable public Account getAccount() {
        return account;
    }

    public BigInteger getSession() {
        return session;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountAndSession that = (AccountAndSession) o;
        return getErrorCode() == that.getErrorCode() &&
            Objects.equals(getAccount(), that.getAccount()) &&
            Objects.equals(getSession(), that.getSession());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccount(), getSession(), getErrorCode());
    }

    @Override
    public String toString() {
        return "AccountAndSession{" +
            "account=" + account +
            ", session=" + session +
            ", errorCode=" + errorCode +
            '}';
    }
}
