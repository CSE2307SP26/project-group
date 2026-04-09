package edu.washu.bank.core;

import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankTest {
    @Test
    void nextAccountIdStartsAtOneAndIncrements() {
        Bank bank = new Bank();

        assertEquals("ACC-0001", bank.nextAccountId());
        assertEquals("ACC-0002", bank.nextAccountId());
    }

    @Test
    void addCustomerAndSaveAccountAreRetrievable() {
        Bank bank = new Bank();
        Customer customer = new Customer("CUST-001", "Alice", "123");
        Account account = new Account("ACC-0001", "CUST-001", AccountType.CHECKING, BigDecimal.ZERO);

        bank.addCustomer(customer);
        bank.saveAccount(account);

        assertTrue(bank.findCustomer("CUST-001").isPresent());
        assertTrue(bank.findAccount("ACC-0001").isPresent());
    }
}
