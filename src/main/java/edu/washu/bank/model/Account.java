package edu.washu.bank.model;

import edu.washu.bank.exception.InvalidTransferException;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final String id;
    private final String customerId;
    private final AccountType type;
    private final BigDecimal balance;

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
        return applyDelta(amount);
    }

    public Account withdraw(BigDecimal amount) {
        validateWithdrawalAmount(amount);
        return applyDelta(amount.negate());
    }

    private Account applyDelta(BigDecimal amountDelta) {
        return new Account(id, customerId, type, balance.add(amountDelta));
    }

    private void validateWithdrawalAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || balance.compareTo(amount) < 0) {
            throw new InvalidTransferException(
                    "Withdrawal amount must be greater than zero and less than or equal to the current balance."
            );
        }
    }
}
