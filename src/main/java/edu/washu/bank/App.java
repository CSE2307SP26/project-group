package edu.washu.bank;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.persistence.SqliteBankStore;
import edu.washu.bank.service.AccountService;

import java.math.BigDecimal;
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

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printUsage(dbPath);
            return;
        }

        if ("create-account".equalsIgnoreCase(args[0])) {
            runCreateAccount(store, bank, accountService, args);
            return;
        }

        if ("check-balance".equalsIgnoreCase(args[0])) {
            runCheckBalance(accountService, args);
            return;
        }

        if ("deposit".equalsIgnoreCase(args[0])) {
            runDeposit(store, bank, accountService, args);
            return;
        }

        if ("clear-data".equalsIgnoreCase(args[0])) {
            runClearData(store, dbPath);
            return;
        }

        System.out.println("Unknown command: " + args[0]);
        printUsage(dbPath);
    }
  
  private static void runWithdraw(AccountService accountService, String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid arguments for deposit.");
            printUsage();
            return;
        }

        String accountId = args[1];

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid deposit amount: " + args[2]);
            return;
        }

        try {
            accountService.withdraw(accountId, amount);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }

    private static void runCreateAccount(
            SqliteBankStore store,
            Bank bank,
            AccountService accountService,
            String[] args
    ) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for create-account.");
            printUsage(SqliteBankStore.resolveDatabasePath());
            return;
        }

        String customerId = args[1];

        AccountType accountType;
        try {
            accountType = AccountType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.out.println("Invalid account type: " + args[2] + ". Use CHECKING or SAVINGS.");
            return;
        }

        BigDecimal openingDeposit;
        try {
            openingDeposit = new BigDecimal(args[3]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid opening deposit: " + args[3]);
            return;
        }

        try {
            Account account = accountService.createAdditionalAccount(customerId, accountType, openingDeposit);
            store.saveFullState(bank);
            System.out.println(
                    "Created account " + account.getId()
                            + " (" + account.getType() + ") for customer " + account.getCustomerId()
                            + " with opening balance " + account.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private static void runCheckBalance(AccountService accountService, String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for check-balance.");
            printUsage(SqliteBankStore.resolveDatabasePath());
            return;
        }

        String accountId = args[1];

        try {
            BigDecimal balance = accountService.getBalance(accountId);
            System.out.println("Account " + accountId + " balance: " + balance);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void runDeposit(
            SqliteBankStore store,
            Bank bank,
            AccountService accountService,
            String[] args
    ) {
        if (args.length != 3) {
            System.out.println("Invalid arguments for deposit.");
            printUsage(SqliteBankStore.resolveDatabasePath());
            return;
        }

        String accountId = args[1];

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid deposit amount: " + args[2]);
            return;
        }

        try {
            Account updatedAccount = accountService.depositIntoExistingAccount(accountId, amount);
            store.saveFullState(bank);
            System.out.println(
                    "Deposited " + amount
                            + " into account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private static void runClearData(SqliteBankStore store, Path dbPath) {
        try {
            store.clearAllAndReseed();
            System.out.println("Cleared database and re-seeded demo customer CUST-001.");
            System.out.println("Database file: " + dbPath.toAbsolutePath().normalize());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private static void printUsage(Path dbPath) {
        System.out.println("Bank CLI (SQLite: " + dbPath.toAbsolutePath().normalize() + ")");
        System.out.println("Override path: -Dbank.db.file=/path/to/bank.db");
        System.out.println("Seeded customer for demo: CUST-001");
        System.out.println("Usage:");
        System.out.println("  create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("  withdraw <accountId> <amount>");
        System.out.println("  check-balance <accountId>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("  clear-data");
        System.out.println("Examples:");
        System.out.println("  create-account CUST-001 CHECKING 100.00");
        System.out.println("  check-balance ACC-0001");
        System.out.println("  deposit ACC-0001 50.00");
        System.out.println("  withdraw ACC-001 25.00");
    }
}