package edu.washu.bank.model;

import java.util.Objects;

public class AdminUser {
    private final String username;
    private final String password;

    public AdminUser(String username, String password) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
