package de.upb.bibifi.verybest.common.messages;

import de.upb.bibifi.verybest.common.models.Action;

import java.math.BigInteger;
import java.util.Objects;

public class BodyMessage {

    private final BigInteger randomNumber;
    private final Action action;

    public BodyMessage(BigInteger randomNumber, Action action) {
        this.randomNumber = randomNumber;
        this.action = action;
    }

    public BigInteger getRandomNumber() {
        return randomNumber;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BodyMessage that = (BodyMessage) o;
        return Objects.equals(getRandomNumber(), that.getRandomNumber()) &&
                Objects.equals(getAction(), that.getAction());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getRandomNumber(), getAction());
    }

    @Override
    public String toString() {
        return "BodyMessage{" +
                "randomNumber=" + randomNumber +
                ", action=" + action +
                '}';
    }
}
