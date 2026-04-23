package edu.washu.bank.core;

import edu.washu.bank.model.Account;
import edu.washu.bank.model.AdminUser;
import edu.washu.bank.model.Customer;
import edu.washu.bank.model.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Bank {
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, AdminUser> admins = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private int accountSequence = 1;
    private int transactionSequence = 1;
    private int customerSequence = 2;

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

    public void removeAccount(String accountId) {
        accounts.remove(accountId);
    }

    public String nextCustomerId() {
        String newId = String.format("CUST-%03d", customerSequence);
        customerSequence += 1;
        return newId;
    }

    public String nextAccountId() {
        String newId = String.format("ACC-%04d", accountSequence);
        accountSequence += 1;
        return newId;
    }

    public String nextTransactionId() {
        String newId = String.format("TXN-%06d", transactionSequence);
        transactionSequence += 1;
        return newId;
    }

    /**
     * Next account id uses this counter; persisted so separate CLI runs stay consistent.
     */
    public int getAccountSequence() {
        return accountSequence;
    }

    public void setAccountSequence(int accountSequence) {
        this.accountSequence = accountSequence;
    }

    public int getTransactionSequence() {
        return transactionSequence;
    }

    public void setTransactionSequence(int transactionSequence) {
        this.transactionSequence = transactionSequence;
    }

    public int getCustomerSequence() {
        return customerSequence;
    }

    public void setCustomerSequence(int customerSequence) {
        this.customerSequence = customerSequence;
    }

    public List<Customer> getCustomersSnapshot() {
        return new ArrayList<>(customers.values());
    }

    public List<Account> getAccountsSnapshot() {
        return new ArrayList<>(accounts.values());
    }

    public void addAdmin(AdminUser adminUser) {
        admins.put(adminUser.getUsername(), adminUser);
    }

    public Optional<AdminUser> findAdmin(String username) {
        return Optional.ofNullable(admins.get(username));
    }

    public List<AdminUser> getAdminsSnapshot() {
        return new ArrayList<>(admins.values());
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public List<Transaction> getTransactionsSnapshot() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> findTransactionsForAccount(String accountId) {
        return transactions.stream()
                .filter(transaction -> transaction.getAccountId().equals(accountId))
                .collect(Collectors.toList());
    }
}