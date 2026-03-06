package edu.washu.bank.service;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.CustomerNotFoundException;
import edu.washu.bank.exception.InvalidOpeningDepositException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
