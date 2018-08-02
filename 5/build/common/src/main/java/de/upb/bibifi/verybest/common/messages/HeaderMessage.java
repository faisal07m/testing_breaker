package de.upb.bibifi.verybest.common.messages;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class HeaderMessage {

    private final String name;
    private final Operation operation;
    private final BigInteger randomNumber;
    private final byte[] userPK;
    private final BigInteger amount;


    /**
     * Constructor for create messages
     *
     * @param name         of the user
     * @param operation    that should be executed
     * @param randomNumber used as nonce
     * @param userPK       that will be used to sign further messages
     * @param amount      of the newly created account
     */
    public HeaderMessage(String name, Operation operation, BigInteger randomNumber, byte[] userPK, BigInteger amount) {
        this.name = name;
        this.operation = operation;
        this.randomNumber = randomNumber;
        this.userPK = userPK;
        this.amount = amount;
    }

    /**
     * Constructor for not create messages (get, deposit, withdraw)
     * <p>
     * During this method, the userPK is set to the empty byte[], the amount is set to 0;
     *
     * @param name         of the user
     * @param operation    that should be executed
     * @param randomNumber used as nonce
     */
    public HeaderMessage(String name, Operation operation, BigInteger randomNumber) {
        this.name = name;
        this.operation = operation;
        this.randomNumber = randomNumber;
        this.userPK = new byte[0];
        this.amount = BigInteger.ZERO;
    }

    public String getName() {
        return name;
    }

    public Operation getOperation() {
        return operation;
    }

    public BigInteger getRandomNumber() {
        return randomNumber;
    }

    public byte[] getUserPK() {
        return userPK;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeaderMessage that = (HeaderMessage) o;
        return Objects.equals(getName(), that.getName()) &&
                        getOperation() == that.getOperation() &&
                        Objects.equals(getRandomNumber(), that.getRandomNumber()) &&
                        Arrays.equals(getUserPK(), that.getUserPK()) &&
                Objects.equals(getAmount(), that.getAmount());
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(getName(), getOperation(), getRandomNumber(), getAmount());
        result = 31 * result + Arrays.hashCode(getUserPK());
        return result;
    }

    @Override
    public String toString() {
        return "HeaderMessage{" +
                "name='" + name + '\'' +
                ", operation=" + operation +
                ", randomNumber=" + randomNumber +
                ", userPK=" + Arrays.toString(userPK) +
                ", amount=" + amount +
                '}';
    }
}
