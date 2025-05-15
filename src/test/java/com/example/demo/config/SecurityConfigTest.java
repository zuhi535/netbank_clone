package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordEncoderBean() {
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
    }
}