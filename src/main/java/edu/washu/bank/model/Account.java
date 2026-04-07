package edu.washu.bank.model;

import java.math.BigDecimal;
import java.util.Objects;
import edu.washu.bank.exception.InvalidTransferException;

public class Account {
    private final String id;
    private final String customerId;
    private final AccountType type;
    private BigDecimal balance;

    public Account(String id, String customerId, AccountType type, BigDecimal balance) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Account deposit(BigDecimal amount) {
        return new Account(id, customerId, type, balance.add(amount));
    }
  
    public void withdraw(BigDecimal amount) {
        if (!isWithdrawalValid(amount)) {
            throw new InvalidTransferException(
                "Withdrawal amount must be greater than zero and less than or equal to the current balance."
            );
        }
        balance = balance.subtract(amount);
    }

    private boolean isWithdrawalValid(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(amount) >= 0;
    }

}
