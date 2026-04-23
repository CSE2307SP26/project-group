package edu.washu.bank.persistence;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.AccountFrozenException;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.TransactionType;
import edu.washu.bank.model.Account;
import edu.washu.bank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteBankStoreTest {
    private static final String SEEDED_CUSTOMER_PASSWORD = "password";
    private static final String MIGRATED_CUSTOMER_PASSWORD = "password";

    @Test
    void loadOrInitializeSeedsDemoCustomer(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);

        Bank bank = store.loadOrInitialize();

        assertTrue(bank.findCustomer("CUST-001").isPresent());
        assertTrue(bank.findAdmin(SqliteBankStore.SEEDED_ADMIN_USERNAME).isPresent());
        assertEquals(1, bank.getAccountSequence());
        assertEquals(1, bank.getTransactionSequence());
    }

    @Test
    void saveAndReloadPersistsAccountsAcrossRuns(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);

        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00"),
                SEEDED_CUSTOMER_PASSWORD
        );
        store.saveFullState(bank);

        SqliteBankStore secondRun = new SqliteBankStore(db);
        Bank reloaded = secondRun.loadOrInitialize();

        assertEquals(
                new BigDecimal("100.00"),
                new AccountService(reloaded).getBalance(account.getId())
        );
    }

    @Test
    void saveAndReloadPersistsTransactionHistoryAndAdminActions(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);

        var first = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00"),
                SEEDED_CUSTOMER_PASSWORD
        );
        var second = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("50.00"),
                SEEDED_CUSTOMER_PASSWORD
        );
        accountService.transfer(first.getId(), second.getId(), new BigDecimal("10.00"), SEEDED_CUSTOMER_PASSWORD);
        accountService.collectFee(
                SqliteBankStore.SEEDED_ADMIN_USERNAME,
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                first.getId(),
                new BigDecimal("5.00")
        );
        accountService.addInterest(
                SqliteBankStore.SEEDED_ADMIN_USERNAME,
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                second.getId(),
                new BigDecimal("2.00")
        );
        store.saveFullState(bank);

        Bank reloaded = new SqliteBankStore(db).loadOrInitialize();
        AccountService reloadedService = new AccountService(reloaded);

        assertTrue(reloaded.findAdmin(SqliteBankStore.SEEDED_ADMIN_USERNAME).isPresent());
        assertEquals(3, reloadedService.getTransactionHistory(first.getId()).size());
        assertEquals(3, reloadedService.getTransactionHistory(second.getId()).size());
        assertEquals(TransactionType.FEE, reloadedService.getTransactionHistory(first.getId()).get(2).getType());
        assertEquals(TransactionType.INTEREST, reloadedService.getTransactionHistory(second.getId()).get(2).getType());
    }

    @Test
    void clearAllAndReseedRemovesAccounts(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);
        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                BigDecimal.TEN,
                SEEDED_CUSTOMER_PASSWORD
        );
        store.saveFullState(bank);

        store.clearAllAndReseed();
        Bank after = store.loadOrInitialize();

        assertTrue(after.findCustomer("CUST-001").isPresent());
        assertTrue(after.findAccount(account.getId()).isEmpty());
        assertTrue(after.findAdmin(SqliteBankStore.SEEDED_ADMIN_USERNAME).isPresent());
        assertEquals(1, after.getAccountSequence());
        assertEquals(1, after.getTransactionSequence());
    }

    @Test
    void loadOrInitializeMigratesLegacyPasswordlessSchema(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("legacy-bank.db");
        SqliteBankStore store = new SqliteBankStore(db);

        try (Connection connection = store.openConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE bank_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
            statement.executeUpdate("INSERT INTO bank_meta (key, value) VALUES ('account_sequence', '1')");
            statement.executeUpdate("CREATE TABLE customers (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)");
            statement.executeUpdate("INSERT INTO customers (id, name) VALUES ('CUST-001', 'Legacy User')");
            statement.executeUpdate(
                    "CREATE TABLE accounts ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "customer_id TEXT NOT NULL,"
                            + "type TEXT NOT NULL,"
                            + "balance TEXT NOT NULL)"
            );
            statement.executeUpdate(
                    "CREATE TABLE transactions ("
                            + "id TEXT PRIMARY KEY NOT NULL,"
                            + "account_id TEXT NOT NULL,"
                            + "type TEXT NOT NULL,"
                            + "amount TEXT NOT NULL,"
                            + "balance_after TEXT NOT NULL,"
                            + "related_account_id TEXT,"
                            + "description TEXT NOT NULL)"
            );
            statement.executeUpdate("CREATE TABLE admins (username TEXT PRIMARY KEY NOT NULL)");
            statement.executeUpdate("INSERT INTO admins (username) VALUES ('admin')");
        }

        Bank bank = store.loadOrInitialize();

        assertEquals(1, bank.getAccountSequence());
        assertEquals(1, bank.getTransactionSequence());
        assertEquals(MIGRATED_CUSTOMER_PASSWORD, bank.findCustomer("CUST-001").orElseThrow().getPassword());
        assertEquals(
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                bank.findAdmin(SqliteBankStore.SEEDED_ADMIN_USERNAME).orElseThrow().getPassword()
        );
    }

    @Test
    void saveAndReloadPersistsSavingsAccountInterestRate(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);

        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                SEEDED_CUSTOMER_PASSWORD
        );

        accountService.setInterestRate(
                SqliteBankStore.SEEDED_ADMIN_USERNAME,
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                account.getId(),
                new BigDecimal("0.05")
        );
        store.saveFullState(bank);

        Bank reloaded = new SqliteBankStore(db).loadOrInitialize();
        Account reloadedAccount = reloaded.findAccount(account.getId()).orElseThrow();

        assertEquals(AccountType.SAVINGS, reloadedAccount.getType());
        assertEquals(new BigDecimal("0.05"), reloadedAccount.getInterestRate());
    }

    @Test
    void saveAndReloadPersistsSavingsAccountInterestRateForViewing(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);

        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                SEEDED_CUSTOMER_PASSWORD
        );

        accountService.setInterestRate(
                SqliteBankStore.SEEDED_ADMIN_USERNAME,
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                account.getId(),
                new BigDecimal("0.05")
        );
        store.saveFullState(bank);

        Bank reloaded = new SqliteBankStore(db).loadOrInitialize();
        AccountService reloadedService = new AccountService(reloaded);

        assertEquals(new BigDecimal("0.05"), reloadedService.getInterestRate(account.getId()));
    }

    @Test
    void saveAndReloadPersistsFrozenAccountState(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);

        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("100.00"),
                SEEDED_CUSTOMER_PASSWORD
        );
        accountService.freezeAccount(
                SqliteBankStore.SEEDED_ADMIN_USERNAME,
                SqliteBankStore.SEEDED_ADMIN_PASSWORD,
                account.getId()
        );
        store.saveFullState(bank);

        Bank reloaded = new SqliteBankStore(db).loadOrInitialize();
        AccountService reloadedService = new AccountService(reloaded);

        assertTrue(reloaded.findAccount(account.getId()).orElseThrow().isFrozen());
        org.junit.jupiter.api.Assertions.assertThrows(
                AccountFrozenException.class,
                () -> reloadedService.depositIntoExistingAccount(account.getId(), BigDecimal.ONE)
        );
    }
}