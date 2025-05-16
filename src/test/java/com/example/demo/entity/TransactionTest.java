package com.example.demo.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    public void testGettersAndSetters() {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setType("deposit");
        t.setAmount(50.0);
        t.setDate(LocalDateTime.now());
        t.setScheduledDate(LocalDateTime.now().plusDays(1));
        t.setExecuted(false);

        assertEquals(1L, t.getId());
        assertEquals("deposit", t.getType());
        assertEquals(50.0, t.getAmount());
        assertNotNull(t.getDate());
        assertNotNull(t.getScheduledDate());
        assertFalse(t.isExecuted());
    }
}
