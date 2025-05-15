package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.Transaction;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDepositPositive() {
        User user = new User();
        user.setUsername("john");
        user.setAccountBalance(100.0);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        userService.deposit("john", 50.0);

        assertEquals(150.0, user.getAccountBalance());
        verify(userRepository).save(user);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testDepositNonPositive() {
        userService.deposit("john", 0);
        verifyNoInteractions(userRepository, transactionRepository);
    }

    @Test
    void testWithdrawSufficientFunds() {
        User user = new User();
        user.setUsername("alice");
        user.setAccountBalance(200.0);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        userService.withdraw("alice", 100.0);

        assertEquals(100.0, user.getAccountBalance());
        verify(userRepository).save(user);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testWithdrawInsufficientFunds() {
        User user = new User();
        user.setUsername("bob");
        user.setAccountBalance(50.0);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        userService.withdraw("bob", 100.0);

        assertEquals(50.0, user.getAccountBalance());
        verify(userRepository, never()).save(user);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testTransferImmediate() {
        User from = new User();
        from.setUsername("u1");
        from.setAccountNumber("ACC1");
        from.setAccountBalance(300.0);

        User to = new User();
        to.setUsername("u2");
        to.setAccountNumber("ACC2");
        to.setAccountBalance(100.0);

        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(from));
        when(userRepository.findAll()).thenReturn(Arrays.asList(from, to));

        userService.transfer("u1", "ACC2", 50.0, null);

        assertEquals(250.0, from.getAccountBalance());
        assertEquals(150.0, to.getAccountBalance());
        verify(userRepository).save(from);
        verify(userRepository).save(to);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void testTransferScheduled() {
        User from = new User();
        from.setUsername("u3");
        from.setAccountNumber("ACC3");
        from.setAccountBalance(500.0);

        User to = new User();
        to.setUsername("u4");
        to.setAccountNumber("ACC4");
        to.setAccountBalance(200.0);

        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        when(userRepository.findByUsername("u3")).thenReturn(Optional.of(from));
        when(userRepository.findAll()).thenReturn(Arrays.asList(from, to));

        userService.transfer("u3", "ACC4", 100.0, futureDate);

        // For scheduled transfer, immediate balances unchanged
        assertEquals(500.0, from.getAccountBalance());
        assertEquals(200.0, to.getAccountBalance());
        verify(transactionRepository, times(1)).save(argThat(tx ->
            tx.getType().equals("transfer") && !tx.isExecuted()
        ));
    }

    @Test
    void testFindByUsernameNotFound() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.findByUsername("nope"));
    }

    @Test
    void testGetTransactionsForUser() {
        User user = new User();
        user.setUsername("t1");
        List<Transaction> list = Arrays.asList(new Transaction(), new Transaction());
        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserOrderByDateDesc(user)).thenReturn(list);

        List<Transaction> result = userService.getTransactionsForUser("t1");
        assertEquals(2, result.size());
    }

    @Test
    void testGetPendingScheduledTransactions() {
        User user = new User();
        user.setUsername("t2");
        List<Transaction> list = Collections.singletonList(new Transaction());
        when(userRepository.findByUsername("t2")).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndExecutedFalseOrderByScheduledDateAsc(user)).thenReturn(list);

        List<Transaction> result = userService.getPendingScheduledTransactions("t2");
        assertEquals(1, result.size());
    }

    @Test
    void testGetAndUpdateScheduledTransaction() {
        User user = new User();
        user.setUsername("u5");
        Transaction tx = new Transaction();
        tx.setExecuted(false);
        when(userRepository.findByUsername("u5")).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(tx));

        Transaction fetched = userService.getScheduledTransaction(1L, "u5");
        assertFalse(fetched.isExecuted());

        LocalDateTime now = LocalDateTime.now();
        userService.updateScheduledTransaction(1L, "u5", now);
        verify(transactionRepository).save(tx);
        assertEquals(now, tx.getScheduledDate());
    }

    @Test
    void testGetScheduledTransactionNotFound() {
        User user = new User();
        user.setUsername("u6");
        when(userRepository.findByUsername("u6")).thenReturn(Optional.of(user));
        when(transactionRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getScheduledTransaction(2L, "u6"));
    }
}
