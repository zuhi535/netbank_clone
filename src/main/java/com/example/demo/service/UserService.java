package com.example.demo.service;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.User;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       TransactionRepository transactionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Regisztrált felhasználó adatainak mentése:
     * - Jelszó bcrypt kódolása
     * - Alapértelmezett szerepkör beállítása
     * - Számlaszám generálása
     * - Kezdeti egyenleg nullázása
     */
    public void saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setAccountNumber(generateAccountNumber());
        user.setAccountBalance(0.0);
        userRepository.save(user);
    }

    private String generateAccountNumber() {
        return "HU" + UUID.randomUUID().toString()
                .substring(0, 10)
                .toUpperCase();
    }

    /**
     * Egyszerű feltöltés a felhasználó számlájára.
     * Hibás (<=0) összeget eldobjuk.
     */
    public void deposit(String username, double amount) {
        if (amount <= 0) return;

        User user = findByUsername(username);
        user.setAccountBalance(user.getAccountBalance() + amount);
        userRepository.save(user);
        saveTransaction(user, "deposit", amount, "Pénz feltöltés",
                LocalDateTime.now(), true);
    }

    /**
     * Egyszerű pénzfelvétel a számláról, ha van rá fedezet.
     */
    public void withdraw(String username, double amount) {
        if (amount <= 0) return;

        User user = findByUsername(username);
        if (user.getAccountBalance() >= amount) {
            user.setAccountBalance(user.getAccountBalance() - amount);
            userRepository.save(user);
            saveTransaction(user, "withdraw", amount, "Pénz levétel",
                    LocalDateTime.now(), true);
        }
    }

    /**
     * Utalás egy másik felhasználónak, akár azonnal akár ütemezetten.
     * scheduledDate == null vagy múltbeli dátum => azonnali.
     */
    public void transfer(String fromUsername,
                         String toAccountNumber,
                         double amount,
                         LocalDateTime scheduledDate) {
        if (amount <= 0) return;

        User fromUser = findByUsername(fromUsername);
        Optional<User> optionalToUser = userRepository.findAll().stream()
                .filter(u -> toAccountNumber.equals(u.getAccountNumber()))
                .findFirst();

        if (optionalToUser.isEmpty()) return;
        User toUser = optionalToUser.get();

        boolean immediate = scheduledDate == null
                || scheduledDate.isBefore(LocalDateTime.now());
        if (immediate && fromUser.getAccountBalance() >= amount) {
            // azonnali átutalás
            fromUser.setAccountBalance(fromUser.getAccountBalance() - amount);
            toUser.setAccountBalance(toUser.getAccountBalance() + amount);
            userRepository.save(fromUser);
            userRepository.save(toUser);

            saveTransaction(fromUser, "transfer", amount,
                    "Utalás erre a számlára: " + toUser.getAccountNumber(),
                    LocalDateTime.now(), true);
            saveTransaction(toUser, "incoming", amount,
                    "Beérkező utalás ettől: " + fromUser.getAccountNumber(),
                    LocalDateTime.now(), true);
        } else {
            // ütemezett utalás
            saveTransaction(fromUser, "transfer", amount,
                    "Ütemezett utalás erre: " + toUser.getAccountNumber(),
                    scheduledDate, false);
        }
    }

    /**
     * Egy korábban mentett, de még végre nem hajtott tranzakció törlése.
     */
    public void cancelScheduledTransaction(Long transactionId, String username) {
        User user = findByUsername(username);
        transactionRepository.findByIdAndUser(transactionId, user)
                .filter(tx -> !tx.isExecuted())
                .ifPresent(transactionRepository::delete);
    }

    /**
     * Ütemezett tranzakció dátumának módosítása.
     */
    public void updateScheduledTransaction(Long transactionId,
                                           String username,
                                           LocalDateTime newDate) {
        Transaction tx = getScheduledTransaction(transactionId, username);
        tx.setScheduledDate(newDate);
        transactionRepository.save(tx);
    }

    /**
     * Visszaad egy még ki nem végrehajtott, azonosított tranzakciót.
     */
    public Transaction getScheduledTransaction(Long id, String username) {
        User user = findByUsername(username);
        return transactionRepository.findByIdAndUser(id, user)
                .filter(tx -> !tx.isExecuted())
                .orElseThrow(() -> new RuntimeException("Nem szerkeszthető tranzakció"));
    }

    /**
     * Periódikusan futtatott ellenőrzés, delegál az executeScheduledTransactions()-nek.
     */
    @Scheduled(fixedRate = 60_000)
    public void checkAndExecuteScheduledTransactions() {
        executeScheduledTransactions();
    }

    /**
     * Betölti a lejárt, még nem végrehajtott tranzakciókat,
     * és ha van fedezet, végrehajtja őket.
     */
    public void executeScheduledTransactions() {
        List<Transaction> pending = transactionRepository.findAll().stream()
                .filter(tx -> !tx.isExecuted()
                        && tx.getScheduledDate().isBefore(LocalDateTime.now()))
                .toList();

        for (Transaction tx : pending) {
            User fromUser = tx.getUser();
            Optional<User> optionalTo = userRepository.findAll().stream()
                    .filter(u -> tx.getDescription().contains(u.getAccountNumber()))
                    .findFirst();

            if (optionalTo.isPresent()
                    && fromUser.getAccountBalance() >= tx.getAmount()) {
                User toUser = optionalTo.get();

                fromUser.setAccountBalance(fromUser.getAccountBalance() - tx.getAmount());
                toUser.setAccountBalance(toUser.getAccountBalance() + tx.getAmount());

                tx.setExecuted(true);
                tx.setDate(LocalDateTime.now());

                saveTransaction(toUser, "incoming", tx.getAmount(),
                        "Beérkező utalás ettől: " + fromUser.getAccountNumber(),
                        LocalDateTime.now(), true);

                userRepository.save(fromUser);
                userRepository.save(toUser);
                transactionRepository.save(tx);
            }
        }
    }

    /**
     * Segédmetódus az összes tranzakció mentésére.
     */
    private void saveTransaction(User user,
                                 String type,
                                 double amount,
                                 String description,
                                 LocalDateTime scheduledDate,
                                 boolean executed) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setScheduledDate(scheduledDate);
        tx.setExecuted(executed);
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    /**
     * Visszaadja a felhasználó összes tranzakcióját dátum szerint csökkenő sorrendben.
     */
    public List<Transaction> getTransactionsForUser(String username) {
        User user = findByUsername(username);
        return transactionRepository.findByUserOrderByDateDesc(user);
    }

    /**
     * Visszaadja a felhasználó függőben lévő, még nem végrehajtott ütemezett tranzakcióit.
     */
    public List<Transaction> getPendingScheduledTransactions(String username) {
        User user = findByUsername(username);
        return transactionRepository.findByUserAndExecutedFalseOrderByScheduledDateAsc(user);
    }

    /**
     * Felhasználó betöltése felhasználónév alapján, hibára dob, ha nincs meg.
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Felhasználó nem található: " + username));
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = findByUsername(username);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(() -> "ROLE_" + user.getRole())
        );
    }

    /**
     * Egyszerű mentő metódus unit tesztekhez:
     * visszatér a repository-tól kapott felhasználóval.
     */
    public User save(User sampleUser) {
        return userRepository.save(sampleUser);
    }
}
