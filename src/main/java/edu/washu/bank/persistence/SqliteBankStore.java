package edu.washu.bank.persistence;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.AdminUser;
import edu.washu.bank.model.Customer;
import edu.washu.bank.model.Transaction;
import edu.washu.bank.model.TransactionType;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Persists {@link Bank} state to a local SQLite file. Each CLI run loads from disk and
 * mutating commands save the full snapshot back (small data set, simple and consistent).
 */
public final class SqliteBankStore {

    public static final String DEFAULT_DB_FILE_PROPERTY = "bank.db.file";
    public static final String DEFAULT_DB_FILENAME = "bank.db";
    public static final String SEEDED_ADMIN_USERNAME = "admin";
    public static final String SEEDED_ADMIN_PASSWORD = "admin123";

    private final String jdbcUrl;

    public SqliteBankStore(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize();
    }

    public Connection openConnection() throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    public void initSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS bank_meta ("
                            + "key TEXT PRIMARY KEY NOT NULL,"
                            + "value TEXT NOT NULL)"
            );
            st.execute(
                    "CREATE TABLE IF NOT EXISTS customers ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "name TEXT NOT NULL,"
                            + "password TEXT NOT NULL DEFAULT 'password')"
            );
            // Migrate existing databases that lack the password column
            try {
                st.execute("ALTER TABLE customers ADD COLUMN password TEXT NOT NULL DEFAULT 'password'");
            } catch (SQLException ignored) {
                // Column already exists — safe to ignore
            }
            st.execute(
                    "CREATE TABLE IF NOT EXISTS accounts ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "customer_id TEXT NOT NULL,"
                            + "type TEXT NOT NULL,"
                            + "balance TEXT NOT NULL,"
                            + "FOREIGN KEY (customer_id) REFERENCES customers(id))"
            );
            st.execute(
                    "CREATE TABLE IF NOT EXISTS transactions ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "account_id TEXT NOT NULL,"
                            + "type TEXT NOT NULL,"
                            + "amount TEXT NOT NULL,"
                            + "balance_after TEXT NOT NULL,"
                            + "related_account_id TEXT,"
                            + "description TEXT NOT NULL)"
            );
            st.execute(
                    "CREATE TABLE IF NOT EXISTS admins ("
                            + "username TEXT PRIMARY KEY NOT NULL,"
                            + "password TEXT NOT NULL)"
            );
        }
    }

    /**
     * Loads bank state from SQLite. If the database has no customers, seeds the demo customer and persists.
     */
    public Bank loadOrInitialize() throws SQLException {
        try (Connection c = openConnection()) {
            initSchema(c);
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM customers")) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    runInTransaction(c, () -> insertSeedRows(c));
                } else {
                    runInTransaction(c, () -> ensureSeedData(c));
                }
            }
            return loadBank(c);
        }
    }

    private static void runInTransaction(Connection c, SqlRunnable action) throws SQLException {
        boolean previous = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            action.run();
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(previous);
        }
    }

    private static void insertSeedRows(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO bank_meta (key, value) VALUES (?, ?)")) {
            ps.setString(1, "account_sequence");
            ps.setString(2, "1");
            ps.executeUpdate();
            ps.setString(1, "transaction_sequence");
            ps.setString(2, "1");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO customers (id, name, password) VALUES (?, ?, ?)")) {
            ps.setString(1, "CUST-001");
            ps.setString(2, "Demo User");
            ps.setString(3, "password");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO admins (username, password) VALUES (?, ?)")) {
            ps.setString(1, SEEDED_ADMIN_USERNAME);
            ps.setString(2, SEEDED_ADMIN_PASSWORD);
            ps.executeUpdate();
        }
    }

    private static void ensureSeedData(Connection c) throws SQLException {
        ensureMetaValue(c, "transaction_sequence", "1");
        ensureAdminUser(c);
    }

    private static void ensureMetaValue(Connection c, String key, String defaultValue) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM bank_meta WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO bank_meta (key, value) VALUES (?, ?)")) {
                        insert.setString(1, key);
                        insert.setString(2, defaultValue);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    private static void ensureAdminUser(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM admins WHERE username = ?")) {
            ps.setString(1, SEEDED_ADMIN_USERNAME);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO admins (username, password) VALUES (?, ?)")) {
                        insert.setString(1, SEEDED_ADMIN_USERNAME);
                        insert.setString(2, SEEDED_ADMIN_PASSWORD);
                        insert.executeUpdate();
                    }
                }
            }
        }
    }

    private static Bank loadBank(Connection c) throws SQLException {
        Bank bank = new Bank();
        try (PreparedStatement ps = c.prepareStatement("SELECT key, value FROM bank_meta")) {
            try (ResultSet rs = ps.executeQuery()) {
                boolean foundAccountSequence = false;
                boolean foundTransactionSequence = false;
                while (rs.next()) {
                    String key = rs.getString("key");
                    String value = rs.getString("value");
                    if ("account_sequence".equals(key)) {
                        bank.setAccountSequence(Integer.parseInt(value));
                        foundAccountSequence = true;
                    } else if ("transaction_sequence".equals(key)) {
                        bank.setTransactionSequence(Integer.parseInt(value));
                        foundTransactionSequence = true;
                    }
                }
                if (!foundAccountSequence) {
                    throw new SQLException("Missing bank_meta.account_sequence");
                }
                if (!foundTransactionSequence) {
                    bank.setTransactionSequence(1);
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement("SELECT id, name, password FROM customers ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bank.addCustomer(new Customer(rs.getString("id"), rs.getString("name"), rs.getString("password")));
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, customer_id, type, balance FROM accounts ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String customerId = rs.getString("customer_id");
                    AccountType type = AccountType.valueOf(rs.getString("type"));
                    BigDecimal balance = new BigDecimal(rs.getString("balance"));
                    Account account = new Account(id, customerId, type, balance);
                    bank.saveAccount(account);
                    bank.findCustomer(customerId).ifPresent(customer -> customer.addAccountId(id));
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement("SELECT username, password FROM admins ORDER BY username")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bank.addAdmin(new AdminUser(rs.getString("username"), rs.getString("password")));
                }
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, account_id, type, amount, balance_after, related_account_id, description "
                        + "FROM transactions ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bank.addTransaction(new Transaction(
                            rs.getString("id"),
                            rs.getString("account_id"),
                            TransactionType.valueOf(rs.getString("type")),
                            new BigDecimal(rs.getString("amount")),
                            new BigDecimal(rs.getString("balance_after")),
                            rs.getString("related_account_id"),
                            rs.getString("description")
                    ));
                }
            }
        }

        return bank;
    }

    /**
     * Writes the full in-memory bank to the database (replaces existing rows).
     */
    public void saveFullState(Bank bank) throws SQLException {
        try (Connection c = openConnection()) {
            initSchema(c);
            runInTransaction(c, () -> {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("DELETE FROM transactions");
                    st.executeUpdate("DELETE FROM admins");
                    st.executeUpdate("DELETE FROM accounts");
                    st.executeUpdate("DELETE FROM customers");
                    st.executeUpdate("DELETE FROM bank_meta");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO bank_meta (key, value) VALUES (?, ?)")) {
                    ps.setString(1, "account_sequence");
                    ps.setString(2, Integer.toString(bank.getAccountSequence()));
                    ps.executeUpdate();
                    ps.setString(1, "transaction_sequence");
                    ps.setString(2, Integer.toString(bank.getTransactionSequence()));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO customers (id, name, password) VALUES (?, ?, ?)")) {
                    for (Customer customer : bank.getCustomersSnapshot()) {
                        ps.setString(1, customer.getId());
                        ps.setString(2, customer.getName());
                        ps.setString(3, customer.getPassword());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO accounts (id, customer_id, type, balance) VALUES (?, ?, ?, ?)")) {
                    for (Account account : bank.getAccountsSnapshot()) {
                        ps.setString(1, account.getId());
                        ps.setString(2, account.getCustomerId());
                        ps.setString(3, account.getType().name());
                        ps.setString(4, account.getBalance().toPlainString());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO admins (username, password) VALUES (?, ?)")) {
                    for (AdminUser adminUser : bank.getAdminsSnapshot()) {
                        ps.setString(1, adminUser.getUsername());
                        ps.setString(2, adminUser.getPassword());
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transactions (id, account_id, type, amount, balance_after, related_account_id, description) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    for (Transaction transaction : bank.getTransactionsSnapshot()) {
                        ps.setString(1, transaction.getId());
                        ps.setString(2, transaction.getAccountId());
                        ps.setString(3, transaction.getType().name());
                        ps.setString(4, transaction.getAmount().toPlainString());
                        ps.setString(5, transaction.getBalanceAfter().toPlainString());
                        ps.setString(6, transaction.getRelatedAccountId());
                        ps.setString(7, transaction.getDescription());
                        ps.executeUpdate();
                    }
                }
            });
        }
    }

    /**
     * Deletes all application data and re-seeds the demo customer (for tests / manual reset).
     */
    public void clearAllAndReseed() throws SQLException {
        try (Connection c = openConnection()) {
            initSchema(c);
            runInTransaction(c, () -> {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("DELETE FROM transactions");
                    st.executeUpdate("DELETE FROM admins");
                    st.executeUpdate("DELETE FROM accounts");
                    st.executeUpdate("DELETE FROM customers");
                    st.executeUpdate("DELETE FROM bank_meta");
                }
                insertSeedRows(c);
            });
        }
    }

    public static Path resolveDatabasePath() {
        String override = System.getProperty(DEFAULT_DB_FILE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(DEFAULT_DB_FILENAME);
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
