package de.upb.cs.bibifi.bankapp.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    @Test
    void testEqualsAndHasCodes() {
        Account acc1 = new Account("310.123123", "acc", "hash1");
        Account acc2 = new Account("310.123123", "acc", "hash1");
        assertTrue(acc1.equals(acc2));
        assertEquals(acc1.hashCode(), acc2.hashCode());
        Account acc3 = new Account("310.123123", "ac", "hash1");
        assertTrue(acc1.equals(acc2));
        assertNotEquals(acc1.hashCode(), acc3.hashCode());
        assertFalse(acc1.equals(acc3));
    }

}