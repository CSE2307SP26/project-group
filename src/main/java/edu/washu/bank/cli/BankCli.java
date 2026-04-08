package edu.washu.bank.cli;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Transaction;
import edu.washu.bank.persistence.SqliteBankStore;
import edu.washu.bank.service.AccountService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class BankCli {
    private final SqliteBankStore store;
    private final Bank bank;
    private final AccountService accountService;
    private final Path dbPath;

    public BankCli(SqliteBankStore store, Bank bank, AccountService accountService, Path dbPath) {
        this.store = store;
        this.bank = bank;
        this.accountService = accountService;
        this.dbPath = dbPath;
    }

    public void run(String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create-account":
                runCreateAccount(args);
                return;
            case "check-balance":
                runCheckBalance(args);
                return;
            case "transaction-history":
                runTransactionHistory(args);
                return;
            case "deposit":
                runDeposit(args);
                return;
            case "withdraw":
                runWithdraw(args);
                return;
            case "close-account":
                runCloseAccount(args);
                return;
            case "transfer":
                runTransfer(args);
                return;
            case "collect-fee":
                runCollectFee(args);
                return;
            case "add-interest":
                runAddInterest(args);
                return;
            case "apply-interest":
                runApplyInterest(args);
                return;
            case "clear-data":
                runClearData();
                return;
            case "set-interest-rate":
                runSetInterestRate(args);
                return;
            default:
                System.out.println("Unknown command: " + args[0]);
                printUsage();
        }
    }

    private void runCreateAccount(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for create-account.");
            printUsage();
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

    private void runCheckBalance(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for check-balance.");
            printUsage();
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

    private void runTransactionHistory(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for transaction-history.");
            printUsage();
            return;
        }

        String accountId = args[1];
        try {
            List<Transaction> history = accountService.getTransactionHistory(accountId);
            System.out.println("Transaction history for account " + accountId + ":");
            if (history.isEmpty()) {
                System.out.println("  No transactions found.");
                return;
            }
            for (Transaction transaction : history) {
                String related = transaction.getRelatedAccountId() == null
                        ? ""
                        : " | related account: " + transaction.getRelatedAccountId();
                System.out.println(
                        "  " + transaction.getId()
                                + " | " + transaction.getType()
                                + " | amount: " + transaction.getAmount()
                                + " | balance after: " + transaction.getBalanceAfter()
                                + related
                                + " | " + transaction.getDescription()
                );
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void runDeposit(String[] args) {
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

    private void runWithdraw(String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid arguments for withdraw.");
            printUsage();
            return;
        }

        String accountId = args[1];

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid withdraw amount: " + args[2]);
            return;
        }

        try {
            Account updatedAccount = accountService.withdraw(accountId, amount);
            store.saveFullState(bank);
            System.out.println(
                    "Withdrew " + amount
                            + " from account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runCloseAccount(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for close-account.");
            printUsage();
            return;
        }

        String accountId = args[1];
        try {
            BigDecimal cashOutAmount = accountService.closeAccount(accountId);
            store.saveFullState(bank);
            System.out.println("Closed account " + accountId + ". Cash-out amount: " + cashOutAmount);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runTransfer(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for transfer.");
            printUsage();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[3]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid transfer amount: " + args[3]);
            return;
        }

        try {
            accountService.transfer(args[1], args[2], amount);
            store.saveFullState(bank);
            System.out.println("Transferred " + amount + " from " + args[1] + " to " + args[2] + ".");
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runCollectFee(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid arguments for collect-fee.");
            printUsage();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[4]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid fee amount: " + args[4]);
            return;
        }

        try {
            Account updatedAccount = accountService.collectFee(args[1], args[2], args[3], amount);
            store.saveFullState(bank);
            System.out.println(
                    "Collected fee of " + amount + " from account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runAddInterest(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid arguments for add-interest.");
            printUsage();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[4]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid interest amount: " + args[4]);
            return;
        }

        try {
            Account updatedAccount = accountService.addInterest(args[1], args[2], args[3], amount);
            store.saveFullState(bank);
            System.out.println(
                    "Added interest of " + amount + " to account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runApplyInterest(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for apply-interest.");
            printUsage();
            return;
        }

        try {
            Account updatedAccount = accountService.applyInterestByRate(args[1], args[2], args[3]);
            store.saveFullState(bank);
            System.out.println(
                    "Applied interest to account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
                            + ", rate: " + updatedAccount.getInterestRate()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runSetInterestRate(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid arguments for set-interest-rate.");
            printUsage();
            return;
        }

        BigDecimal interestRate;
        try {
            interestRate = new BigDecimal(args[4]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid interest rate: " + args[4]);
            return;
        }

        try {
            Account updatedAccount = accountService.setInterestRate(args[1], args[2], args[3], interestRate);
            store.saveFullState(bank);
            System.out.println(
                    "Set interest rate for account " + updatedAccount.getId()
                            + " to " + updatedAccount.getInterestRate()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runClearData() {
        try {
            store.clearAllAndReseed();
            System.out.println("Cleared database and re-seeded demo customer CUST-001.");
            System.out.println("Database file: " + dbPath.toAbsolutePath().normalize());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void printUsage() {
        System.out.println("Bank CLI (SQLite: " + dbPath.toAbsolutePath().normalize() + ")");
        System.out.println("Override path: -Dbank.db.file=/path/to/bank.db");
        System.out.println("Seeded customer for demo: CUST-001");
        System.out.println(
                "Seeded admin for demo: "
                        + SqliteBankStore.SEEDED_ADMIN_USERNAME
                        + " / "
                        + SqliteBankStore.SEEDED_ADMIN_PASSWORD
        );
        System.out.println("Usage:");
        System.out.println("  create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>");
        System.out.println("  transaction-history <accountId>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("  withdraw <accountId> <amount>");
        System.out.println("  close-account <accountId>");
        System.out.println("  transfer <fromAccountId> <toAccountId> <amount>");
        System.out.println("  collect-fee <adminUsername> <adminPassword> <accountId> <amount>");
        System.out.println("  add-interest <adminUsername> <adminPassword> <accountId> <amount>");
        System.out.println("  check-balance <accountId>");
        System.out.println("  clear-data");
        System.out.println("  set-interest-rate <adminUsername> <adminPassword> <accountId> <rate>");
        System.out.println("  apply-interest <adminUsername> <adminPassword> <accountId>");
        System.out.println("Examples:");
        System.out.println("  create-account CUST-001 CHECKING 100.00");
        System.out.println("  check-balance ACC-0001");
        System.out.println("  deposit ACC-0001 50.00");
        System.out.println("  withdraw ACC-0001 25.00");
        System.out.println("  transaction-history ACC-0001");
        System.out.println("  transfer ACC-0001 ACC-0002 10.00");
        System.out.println("  collect-fee admin admin123 ACC-0001 5.00");
        System.out.println("  add-interest admin admin123 ACC-0001 3.00");
        System.out.println("  set-interest-rate admin admin123 ACC-0001 0.05");
        System.out.println("  apply-interest admin admin123 ACC-0001");
    }
}
