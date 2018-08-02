package de.upb.bionicbeaver.bank.tx;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The user model, which contains all the relevant information regarding the
 * bank user.
 *
 * It provides methods to manage the bank account of the user.
 *
 * @author Jerome Kempf, Thomas Wiens
 * @version 0.1
 * @since 2018-07-26
 */
@Getter
public class User {
    /**
     * User identifier
     */
    private final String accountName;

    /**
     * Tuple, which contains information regarding the card file data of the user.
     * This information is used, to authenticate the user.
     *
     * The tuple has the following structure:
     *      Cardname, Hashed Cardfile Content
     */
    private final String card;

    @Setter(value = AccessLevel.PACKAGE)
    private BigDecimal balance;

    @Getter(AccessLevel.PRIVATE)
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * To ensure integrity during concurrent transactions this lock is used before making
     * any credits/debits (tx) to the account balance.
     */
    private final AtomicBoolean lock = new AtomicBoolean(false);

    /**
     * Constructor, which initializes the attributes of a user.
     *
     * Gets the secret card file as an input and hashes the content
     * together with a randomly generated salt.
     * Only this hash is stored in the @see cardData tuple.
     *
     * @param accountName The account name of the user.
     * @param card The card of the user, with the secret content.
     * @param balance The initial balance of the user.
     */
    public User(String accountName, String card, BigDecimal balance) {
        this.accountName = accountName;
        this.card = card;
        this.balance = balance;
    }

    void addTransactionValue(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    void enableAccount() {
        if(!this.enabled.get()) {
            this.enabled.getAndSet(true);
        }
    }

    boolean isEnabled() {
        return this.enabled.get();
    }
}
