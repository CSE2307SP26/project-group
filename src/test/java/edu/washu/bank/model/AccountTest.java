package edu.washu.bank.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
    

    private Account account(double balance) {
        return new Account("ACC-0001", "CUST-001", AccountType.CHECKING, new BigDecimal(balance));
    }

    // test getter
    @Test
    void alertThresholdIsFixedAtFifty() {
        assertEquals(new BigDecimal("50.00"), account(100).getAlertBalanceThreshold());
    }

    // test if there's no alert when withdrawing above threshold
    @Test
    void withdrawAboveThresholdPrintsNoAlert() {
        ByteArrayOutputStream out = captureStdout();
        account(200).withdraw(new BigDecimal("100"));
        assertTrue(out.toString().isEmpty());
    }

    // test if there's no alert when withdrawing to exactly the threshold
    @Test
    void withdrawThatLandsExactlyOnThresholdPrintsNoAlert() {
        // balance - amount == 50.00, condition is strictly < threshold, so no alert
        ByteArrayOutputStream out = captureStdout();
        account(100).withdraw(new BigDecimal("50"));
        assertTrue(out.toString().isEmpty());
    }

    // test if there's an alert when withdrawing below the threshold
    @Test
    void withdrawThatDropsBelowThresholdPrintsAlert() {
        ByteArrayOutputStream out = captureStdout();
        account(100).withdraw(new BigDecimal("51"));
        assertTrue(out.toString().contains("Alert"));
    }

    // test if there's an alert when withdrawing below the threshold when already below the threshold
    @Test
    void withdrawWhenAlreadyBelowThresholdPrintsAlert() {
        ByteArrayOutputStream out = captureStdout();
        account(40).withdraw(new BigDecimal("10"));
        assertTrue(out.toString().contains("Alert"));
    }

    // test if the returned account has the correct updated balance after withdraw
    @Test
    void withdrawReturnsAccountWithUpdatedBalance() {
        Account updated = account(100).withdraw(new BigDecimal("30"));
        assertEquals(new BigDecimal("70.0"), updated.getBalance());
    }

    // TODO: should add more test on threshold change and balance 
    // if the threshold if configurable in the future
    
    // -- helpers --

    private ByteArrayOutputStream captureStdout() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        return out;
    }
}
