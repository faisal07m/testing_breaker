package de.upb.bibifi.verybest.common.util;

import java.security.SecureRandom;
import java.util.Random;

public class RandomProvider {
    public static Random provideSecure() {
        return new SecureRandom();
    }
}
