package edu.washu.bank.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Transaction {
    private final String id;
    private final String accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String relatedAccountId;
    private final String description;

    public Transaction(
            String id,
            String accountId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String relatedAccountId,
            String description
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "balanceAfter must not be null");
        this.relatedAccountId = relatedAccountId;
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getRelatedAccountId() {
        return relatedAccountId;
    }

    public String getDescription() {
        return description;
    }
}
