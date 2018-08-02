package de.upb.cs.bibifi.commons;

import de.upb.cs.bibifi.commons.validator.InputPatternChecker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputCheckerTest {

    @Test
    public void testSampleInput(){
        String validInput1 = "-n123.1313 -c bob.card -abob";
        assertTrue(InputPatternChecker.check(validInput1));

        String validInput2 = "-c bob.card -w12.13 -i128.1.2.3";
        assertTrue(InputPatternChecker.check(validInput2));

        String validInput3 = "-c bob.card -w12.13 -i 128.1.2.3";
        assertTrue(InputPatternChecker.check(validInput3));

        String validInput4 = "-c bob.card -g -abob";
        assertTrue(InputPatternChecker.check(validInput4));

        String invalidInput1 = "-c bob.card -g -n 12 -abob";
        assertFalse(InputPatternChecker.check(invalidInput1));

        String invalidInput2 = "-c bob.card -abob";
        assertFalse(InputPatternChecker.check(invalidInput2));

        String validInput5 = "-c bob.card -d 13.131312 -a bob";
        assertTrue(InputPatternChecker.check(validInput5));

        String validInput6 = "-abos -n13.13";
        assertTrue(InputPatternChecker.check(validInput6));

        String invalidInput4 = "-c bob.card -n 12 -abob -d";
        assertFalse(InputPatternChecker.check(invalidInput4));
    }
}
