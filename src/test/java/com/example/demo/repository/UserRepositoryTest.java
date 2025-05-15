package com.example.demo.repository;

import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testSaveAndFindByUsername() {
        User u = new User();
        u.setAccountBalance(0.0);
        u.setAccountNumber("ACC");
        u.setUsername("tester");
        u.setPassword("pwd");
        userRepository.save(u);

        Optional<User> found = userRepository.findByUsername("tester");
        assertTrue(found.isPresent());
        assertEquals("tester", found.get().getUsername());
    }
}
