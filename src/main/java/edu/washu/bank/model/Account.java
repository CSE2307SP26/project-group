package edu.washu.bank.model;

import edu.washu.bank.exception.InvalidDepositAmountException;
import edu.washu.bank.exception.InvalidTransferException;

import java.math.BigDecimal;
import java.util.Objects;

public class Account {
    private final String id;
    private final String customerId;
    private final AccountType type;
    private final BigDecimal balance;
    private final boolean frozen;

    public Account(String id, String customerId, AccountType type, BigDecimal balance) {
        this(id, customerId, type, balance, false);
    }

    public Account(String id, String customerId, AccountType type, BigDecimal balance, boolean frozen) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
        this.frozen = frozen;
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

    public boolean isFrozen() {
        return frozen;
    }

    public Account deposit(BigDecimal amount) {
        validateDepositAmount(amount);
        return applyDelta(amount);
    }

    public Account withdraw(BigDecimal amount) {
        validateWithdrawalAmount(amount);
        return applyDelta(amount.negate());
    }

    public Account freeze() {
        return new Account(id, customerId, type, balance, true);
    }

    public Account unfreeze() {
        return new Account(id, customerId, type, balance, false);
    }

    private Account applyDelta(BigDecimal amountDelta) {
        return new Account(id, customerId, type, balance.add(amountDelta), frozen);
    }

    private void validateDepositAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDepositAmountException("Deposit amount must be greater than 0");
        }
    }

    private void validateWithdrawalAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Withdrawal amount must be greater than zero.");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InvalidTransferException("Insufficient funds.");
        }
    }
}
