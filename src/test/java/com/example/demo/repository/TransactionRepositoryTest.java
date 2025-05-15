package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindByUserOrderByDateDesc() {
        User u = new User();
        u.setAccountBalance(0.0);
        u.setAccountNumber("ACC");
        u.setUsername("u1");
        userRepository.save(u);
        Transaction t = new Transaction();
        t.setUser(u);
        t.setDate(LocalDateTime.now());
        transactionRepository.save(t);

        List<Transaction> list = transactionRepository.findByUserOrderByDateDesc(u);
        assertFalse(list.isEmpty());
        assertEquals(u, list.get(0).getUser());
    }

    @Test
    public void testFindByUserAndExecutedFalseOrderByScheduledDateAsc() {
        User u = new User();
        u.setAccountBalance(0.0);
        u.setAccountNumber("ACC");
        u.setUsername("u2");
        userRepository.save(u);
        Transaction t = new Transaction();
        t.setUser(u);
        t.setExecuted(false);
        t.setScheduledDate(LocalDateTime.now().plusDays(1));
        transactionRepository.save(t);

        List<Transaction> pending = transactionRepository.findByUserAndExecutedFalseOrderByScheduledDateAsc(u);
        assertFalse(pending.isEmpty());
    }

    @Test
    public void testFindByIdAndUser() {
        User u = new User();
        u.setAccountBalance(0.0);
        u.setAccountNumber("ACC");
        u.setUsername("u3");
        userRepository.save(u);
        Transaction t = new Transaction();
        t.setUser(u);
        transactionRepository.save(t);

        Optional<Transaction> opt = transactionRepository.findByIdAndUser(t.getId(), u);
        assertTrue(opt.isPresent());
    }
}
