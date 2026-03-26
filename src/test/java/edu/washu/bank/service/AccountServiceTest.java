package edu.washu.bank.service;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.CustomerNotFoundException;
import edu.washu.bank.exception.InvalidOpeningDepositException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import edu.washu.bank.exception.AccountNotFoundException;
import edu.washu.bank.exception.InvalidDepositAmountException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {
    private Bank bank;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.addCustomer(new Customer("CUST-001", "Alice"));
        accountService = new AccountService(bank);
    }

    @Test
    void createAdditionalAccountForExistingCustomerSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("250.00")
        );

        assertEquals("CUST-001", account.getCustomerId());
        assertEquals(new BigDecimal("250.00"), account.getBalance());
        assertEquals(AccountType.CHECKING, account.getType());
        assertTrue(bank.findAccount(account.getId()).isPresent());
    }

    @Test
    void createAdditionalAccountAddsAccountToCustomer() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("50.00")
        );

        Customer customer = bank.findCustomer("CUST-001").orElseThrow();
        assertEquals(1, customer.getAccountIds().size());
        assertEquals(account.getId(), customer.getAccountIds().get(0));
    }

    @Test
    void createAdditionalAccountForMissingCustomerThrows() {
        assertThrows(
                CustomerNotFoundException.class,
                () -> accountService.createAdditionalAccount("CUST-404", AccountType.CHECKING, BigDecimal.ZERO)
        );
    }

    @Test
    void createAdditionalAccountWithNegativeDepositThrows() {
        assertThrows(
                InvalidOpeningDepositException.class,
                () -> accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("-1.00"))
        );
    }

    @Test
    void createMultipleAccountsUsesDeterministicSequentialIds() {
        Account first = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, BigDecimal.ZERO);
        Account second = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, BigDecimal.ZERO);

        assertEquals("ACC-0001", first.getId());
        assertEquals("ACC-0002", second.getId());
    }

    @Test
    void withdrawWithSufficientFundsSucceeds() {
        Account account = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("100.00"));
        accountService.withdraw(account.getId(), new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), account.getBalance());
    }

    @Test
    void withdrawWithInsufficientFundsThrows() {
        Account account = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("50.00"));
        assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("60.00"))
        ); 
    }

    @Test
    void withdrawWithNegativeAmountThrows() {
        Account account = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("50.00"));
        assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("-10.00"))
        );
    }

    @Test
    void withdrawWithInvalidAmountThrows() {
        Account account = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("50.00"));
        assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw(account.getId(), null)
        );
    }

    @ Test
    void withdrawFromNonexistentAccountThrows() {
        assertThrows(
                RuntimeException.class,
                () -> accountService.withdraw("ACC-404", new BigDecimal("10.00"))        );
    }

    @Test
    void getBalanceForExistingAccountReturnsCorrectBalance() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("500.00")
        );

        BigDecimal balance = accountService.getBalance(account.getId());

        assertEquals(new BigDecimal("500.00"), balance);
    }

    @Test
    void getBalanceForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getBalance("ACC-9999")
        );
    }

    @Test
    void depositIntoExistingAccountSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00")
        );

        Account updatedAccount = accountService.depositIntoExistingAccount(
                account.getId(),
                new BigDecimal("50.00")
        );

        assertEquals(new BigDecimal("150.00"), updatedAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), bank.findAccount(account.getId()).orElseThrow().getBalance());
    }

    @Test
    void depositIntoMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.depositIntoExistingAccount("ACC-9999", new BigDecimal("50.00"))
        );
    }

    @Test
    void depositWithZeroAmountThrows() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00")
        );

        assertThrows(
                InvalidDepositAmountException.class,
                () -> accountService.depositIntoExistingAccount(account.getId(), BigDecimal.ZERO)
        );
    }

    @Test
    void depositWithNegativeAmountThrows() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00")
        );

        assertThrows(
                InvalidDepositAmountException.class,
                () -> accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("-10.00"))
        );
    }
}
