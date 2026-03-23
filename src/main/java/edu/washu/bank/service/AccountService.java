package edu.washu.bank.service;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.AccountNotFoundException;
import edu.washu.bank.exception.CustomerNotFoundException;
import edu.washu.bank.exception.InvalidOpeningDepositException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;

import java.math.BigDecimal;
import java.util.Objects;

public class AccountService {
    private final Bank bank;

    public AccountService(Bank bank) {
        this.bank = Objects.requireNonNull(bank, "bank must not be null");
    }

    public Account createAdditionalAccount(String customerId, AccountType accountType, BigDecimal openingDeposit) {
        if (openingDeposit == null || openingDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOpeningDepositException("Opening deposit must be at least 0");
        }

        Customer customer = bank.findCustomer(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        String accountId = bank.nextAccountId();
        Account account = new Account(accountId, customer.getId(), accountType, openingDeposit);
        bank.saveAccount(account);
        customer.addAccountId(accountId);
        return account;
    }
    public BigDecimal getBalance(String accountId) {
        Account account = bank.findAccount(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return account.getBalance();
    }
}
