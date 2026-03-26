package edu.washu.bank.persistence;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;

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
                            + "name TEXT NOT NULL)"
            );
            st.execute(
                    "CREATE TABLE IF NOT EXISTS accounts ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "customer_id TEXT NOT NULL,"
                            + "type TEXT NOT NULL,"
                            + "balance TEXT NOT NULL,"
                            + "FOREIGN KEY (customer_id) REFERENCES customers(id))"
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
                "INSERT INTO bank_meta (key, value) VALUES ('account_sequence', ?)")) {
            ps.setString(1, "1");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO customers (id, name) VALUES (?, ?)")) {
            ps.setString(1, "CUST-001");
            ps.setString(2, "Demo User");
            ps.executeUpdate();
        }
    }

    private static Bank loadBank(Connection c) throws SQLException {
        Bank bank = new Bank();
        int sequence;
        try (PreparedStatement ps = c.prepareStatement("SELECT value FROM bank_meta WHERE key = 'account_sequence'")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Missing bank_meta.account_sequence");
                }
                sequence = Integer.parseInt(rs.getString(1));
            }
        }
        bank.setAccountSequence(sequence);

        try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM customers ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bank.addCustomer(new Customer(rs.getString("id"), rs.getString("name")));
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
                    st.executeUpdate("DELETE FROM accounts");
                    st.executeUpdate("DELETE FROM customers");
                    st.executeUpdate("DELETE FROM bank_meta");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO bank_meta (key, value) VALUES ('account_sequence', ?)")) {
                    ps.setString(1, Integer.toString(bank.getAccountSequence()));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO customers (id, name) VALUES (?, ?)")) {
                    for (Customer customer : bank.getCustomersSnapshot()) {
                        ps.setString(1, customer.getId());
                        ps.setString(2, customer.getName());
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
