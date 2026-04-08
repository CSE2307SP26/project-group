package edu.washu.bank;

import edu.washu.bank.cli.BankCli;
import edu.washu.bank.core.Bank;
import edu.washu.bank.persistence.SqliteBankStore;
import edu.washu.bank.service.AccountService;

import java.nio.file.Path;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) {
        Path dbPath = SqliteBankStore.resolveDatabasePath();
        SqliteBankStore store = new SqliteBankStore(dbPath);
        Bank bank;
        try {
            bank = store.loadOrInitialize();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.exit(1);
            return;
        }

        AccountService accountService = new AccountService(bank);
        new BankCli(store, bank, accountService, dbPath).run(args);
    }
}