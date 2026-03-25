package edu.washu.bank.persistence;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteBankStoreTest {

    @Test
    void loadOrInitializeSeedsDemoCustomer(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);

        Bank bank = store.loadOrInitialize();

        assertTrue(bank.findCustomer("CUST-001").isPresent());
        assertEquals(1, bank.getAccountSequence());
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
                new BigDecimal("100.00")
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
    void clearAllAndReseedRemovesAccounts(@TempDir Path tempDir) throws SQLException {
        Path db = tempDir.resolve("bank.db");
        SqliteBankStore store = new SqliteBankStore(db);
        Bank bank = store.loadOrInitialize();
        AccountService accountService = new AccountService(bank);
        var account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                BigDecimal.TEN
        );
        store.saveFullState(bank);

        store.clearAllAndReseed();
        Bank after = store.loadOrInitialize();

        assertTrue(after.findCustomer("CUST-001").isPresent());
        assertTrue(after.findAccount(account.getId()).isEmpty());
        assertEquals(1, after.getAccountSequence());
    }
}
