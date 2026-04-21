package edu.washu.bank.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Customer {
    private final String id;
    private final String name;
    private final String password;
    private final List<String> accountIds = new ArrayList<>();

    public Customer(String id, String name, String password) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getAccountIds() {
        return Collections.unmodifiableList(accountIds);
    }

    public void addAccountId(String accountId) {
        accountIds.add(Objects.requireNonNull(accountId, "accountId must not be null"));
    }

    public void removeAccountId(String accountId) {
        accountIds.remove(Objects.requireNonNull(accountId, "accountId must not be null"));
    }
}
