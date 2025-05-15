package com.example.demo.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testGettersAndSetters() {
        User u = new User();
        u.setUsername("john");
        u.setPassword("pass");
        u.setAccountNumber("ACC123");
        u.setAccountBalance(100.0);

        assertEquals("john", u.getUsername());
        assertEquals("pass", u.getPassword());
        assertEquals("ACC123", u.getAccountNumber());
        assertEquals(100.0, u.getAccountBalance());
    }
}
