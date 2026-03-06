package edu.washu.bank.core;

import edu.washu.bank.model.Account;
import edu.washu.bank.model.Customer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Bank {
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private int accountSequence = 1;

    public void addCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    public Optional<Customer> findCustomer(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    public void saveAccount(Account account) {
        accounts.put(account.getId(), account);
    }

    public Optional<Account> findAccount(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public String nextAccountId() {
        String newId = String.format("ACC-%04d", accountSequence);
        accountSequence += 1;
        return newId;
    }
}
