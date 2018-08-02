package de.upb.bibifi.verybest.common.messages;

import java.math.BigInteger;
import java.util.Objects;

public class ChallengeMessage {

    private final BigInteger randomNumber;

    public ChallengeMessage(BigInteger randomNumber) {
        this.randomNumber = randomNumber;
    }

    public BigInteger getRandomNumber() {
        return randomNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengeMessage that = (ChallengeMessage) o;
        return Objects.equals(getRandomNumber(), that.getRandomNumber());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getRandomNumber());
    }

    @Override
    public String toString() {
        return "ChallengeMessage{" +
                "randomNumber=" + randomNumber +
                '}';
    }
}
