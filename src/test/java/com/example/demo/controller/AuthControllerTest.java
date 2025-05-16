package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(AuthController.class)

@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private Principal principal = () -> "john";

    @Test
    public void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    public void testRegisterUser() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "john")
                        .param("password", "pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(userService).saveUser(any(User.class));
    }

    @Test
    public void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testDepositEndpoint() throws Exception {
        mockMvc.perform(post("/deposit").principal(principal).param("amount", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        verify(userService).deposit("john", 100.0);
    }

    @Test
    public void testWithdrawEndpoint() throws Exception {
        mockMvc.perform(post("/withdraw").principal(principal).param("amount", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        verify(userService).withdraw("john", 50.0);
    }

    @Test
    public void testTransferWithoutSchedule() throws Exception {
        mockMvc.perform(post("/transfer")
                        .principal(principal)
                        .param("targetAccountNumber", "ACC1")
                        .param("amount", "25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        verify(userService).transfer("john", "ACC1", 25.0, null);
    }

    @Test
    public void testTransferWithSchedule() throws Exception {
        String date = "2025-01-01";
        mockMvc.perform(post("/transfer")
                        .principal(principal)
                        .param("targetAccountNumber", "ACC1")
                        .param("amount", "25")
                        .param("date", date)
                        .param("hour", "10")
                        .param("minute", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).transfer(eq("john"), eq("ACC1"), eq(25.0), captor.capture());
    }

    @Test
    public void testCancelTransaction() throws Exception {
        mockMvc.perform(post("/cancelTransaction").principal(principal).param("transactionId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        verify(userService).cancelScheduledTransaction(1L, "john");
    }

    @Test
    public void testUpdateTransaction() throws Exception {
        String date = "2025-02-02";
        mockMvc.perform(post("/updateTransaction").principal(principal)
                        .param("transactionId", "1")
                        .param("date", date)
                        .param("hour", "12")
                        .param("minute", "45"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).updateScheduledTransaction(eq(1L), eq("john"), captor.capture());
    }
}
