package de.upb.cs.bibifi.bankapp.bank;

import java.math.BigDecimal;

public interface IBank {
    void startup(String authFileName) throws Exception;

    String createBalance(String acc, String balance);

    boolean deposit(String acc, String pin, String balance);

    boolean withdraw(String acc, String pin, String balance);

    BigDecimal checkBalance(String acc, String pin);

    void commit();

    void undo();
}
