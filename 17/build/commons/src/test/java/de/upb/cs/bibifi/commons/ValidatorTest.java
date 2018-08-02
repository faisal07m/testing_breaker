package de.upb.cs.bibifi.commons;

import de.upb.cs.bibifi.commons.validator.Validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.*;

class ValidatorTest {

    @Test
    void testValidateNumerals() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validateNumerals("45.68"));
        Assertions.assertFalse(validator.validateNumerals("45.686"));
    }

    @Test
    void testValidateIP() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validateIP("45.68.0.0"));
        Assertions.assertFalse(validator.validateIP("456.686.78.0"));
    }

    @Test
    void testValidatePort() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validatePort("5670"));
        Assertions.assertFalse(validator.validatePort("75"));
    }

    @Test
    void testValidateFileName() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validateFileName(".fdrr5"));
        Assertions.assertFalse(validator.validateFileName("."));
    }

    @Test
    void testValidateAccountName() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validateAccountName("."));
        Assertions.assertFalse(validator.validateAccountName("?f#"));
    }

    @Test
    void testValidateInitialBalance() {
        Validator validator = new Validator();
        Assertions.assertTrue(validator.validateInitialBalance("67"));
        Assertions.assertFalse(validator.validateInitialBalance("2"));
    }

    @Test
    void testValidateArgumentLength() {
        Validator validator = new Validator();
        final String strCharacter = "*";

        int validNum = 4095;
        int invalidNum = 4098;

        StringBuilder sbt = new StringBuilder(validNum);
        StringBuilder sbf = new StringBuilder(invalidNum);

        for (int i = 0; i < invalidNum; i++) {
            sbf.append(strCharacter);
        }

        for (int i = 0; i < validNum; i++) {
            sbt.append(strCharacter);
        }

        String[] argArray1 = new String[1];
        argArray1[0] = sbt.toString();

        String[] argArray2 = new String[1];
        argArray2[0] = sbf.toString();

        Assertions.assertTrue(validator.validateArgumentLength(argArray1));
        Assertions.assertFalse(validator.validateArgumentLength(argArray2));
    }

    @Test
    void testCheckDuplicates() {
        String strValid = "c";
        String strInvalid = "a";
        Set<String> strSet = new HashSet<String>(Arrays.asList("a", "g", "n"));
        Validator validator = new Validator();
        Assertions.assertTrue(validator.checkDuplicates(strValid, strSet));
        Assertions.assertFalse(validator.checkDuplicates(strInvalid, strSet));
    }

    @Test
    void testCheckOperations() {
        Set<String> strSetValid = new HashSet<String>(Arrays.asList("a", "g"));
        Set<String> strSetInvalid = new HashSet<String>(Arrays.asList("a", "g", "w"));
        Validator validator = new Validator();
        Assertions.assertTrue(validator.checkOperations(strSetValid));
        Assertions.assertFalse(validator.checkOperations(strSetInvalid));
    }

}
