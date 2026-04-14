package edu.washu.bank.cli;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.AuthenticationException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;
import edu.washu.bank.model.Transaction;
import edu.washu.bank.persistence.SqliteBankStore;
import edu.washu.bank.service.AccountService;
import edu.washu.bank.model.Customer;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class BankCli {
    private final SqliteBankStore store;
    private final Bank bank;
    private final AccountService accountService;
    private final Path dbPath;

    private final Scanner scanner = new Scanner(System.in);

    public BankCli(SqliteBankStore store, Bank bank, AccountService accountService, Path dbPath) {
        this.store = store;
        this.bank = bank;
        this.accountService = accountService;
        this.dbPath = dbPath;
    }

    public void run() {
        while (true) {
            loginMenu();
        }
    }

<<<<<<< UI-reconstruct
    // ─── Login ────────────────────────────────────────────────────────────────

    private void loginMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("          Welcome to WashU Bank         ");
        System.out.println("========================================");
        System.out.println("1. Customer Login");
        System.out.println("2. Admin Login");
        System.out.println("0. Exit");

        int selection = getUserSelection(2);
        switch (selection) {
            case 1: customerLogin(); break;
            case 2: adminLogin(); break;
            case 0:
                System.out.println("Goodbye!");
                System.exit(0);
        }
    }

    private void customerLogin() {
        System.out.print("Enter Customer ID: ");
        String customerId = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        Customer customer = bank.findCustomer(customerId).orElse(null);
        if (customer == null || !customer.getPassword().equals(password)) {
            System.out.println("Invalid customer ID or password.");
            return;
        }

        System.out.println("Welcome, " + customer.getName() + "!");
        customerMenu(customer);
    }

    private void adminLogin() {
        System.out.print("Enter Admin Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        boolean valid = bank.findAdmin(username)
                .map(admin -> admin.getPassword().equals(password))
                .orElse(false);

        if (!valid) {
            System.out.println("Invalid admin credentials.");
            return;
        }

        System.out.println("Welcome, Admin " + username + "!");
        adminMenu(username, password);
    }

    // ─── Customer Menu ────────────────────────────────────────────────────────

    private void customerMenu(Customer customer) {
        while (true) {
            System.out.println();
            System.out.println("--- Customer Menu (" + customer.getName() + ") ---");
            System.out.println("1. View My Accounts");
            System.out.println("2. Open New Account");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer");
            System.out.println("6. Transaction History");
            System.out.println("7. Close Account");
            System.out.println("0. Logout");

            int selection = getUserSelection(7);
            switch (selection) {
                case 1: viewAccounts(customer); break;
                case 2: openNewAccount(customer); break;
                case 3: deposit(customer); break;
                case 4: withdraw(customer); break;
                case 5: transfer(customer); break;
                case 6: transactionHistory(customer); break;
                case 7: closeAccount(customer); break;
                case 0:
                    System.out.println("Logged out.");
                    return;
            }
=======
        switch (args[0].toLowerCase()) {
            case "create-account":
                runCreateAccount(args);
                return;
            case "check-balance":
                runCheckBalance(args);
                return;
            case "total-balance":
                runTotalBalance(args);
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
            case "view-interest-rate":
                runViewInterestRate(args);
                return;
            case "freeze-account":
                runFreezeAccount(args);
                return;
            case "unfreeze-account":
                runUnfreezeAccount(args);
                return;
            case "clear-data":
                runClearData();
                return;
            case "set-interest-rate":
                runSetInterestRate(args);
                return;
            case "list-customers":
                runListCustomers(args);
                return;
            case "list-accounts":
                runListAccounts(args);
                return;
            default:
                System.out.println("Unknown command: " + args[0]);
                printUsage();
        }
    }

    private void runCreateAccount(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid arguments for create-account.");
            printUsage();
            return;
        }

        String customerId = args[1];
        String password = args[2];

        AccountType accountType;
        try {
            accountType = AccountType.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.out.println("Invalid account type: " + args[3] + ". Use CHECKING or SAVINGS.");
            return;
        }

        BigDecimal openingDeposit;
        try {
            openingDeposit = new BigDecimal(args[4]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid opening deposit: " + args[4]);
            return;
        }

        try {
            Account account = accountService.createAdditionalAccount(customerId, accountType, openingDeposit, password);
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
>>>>>>> main
        }
    }

    private void viewAccounts(Customer customer) {
        List<String> accountIds = customer.getAccountIds();
        if (accountIds.isEmpty()) {
            System.out.println("You have no accounts.");
            return;
        }
        System.out.println();
        System.out.println("Your Accounts:");
        for (String accountId : accountIds) {
            bank.findAccount(accountId).ifPresent(a ->
                System.out.printf("  %-10s  %-10s  Balance: %s%n",
                        a.getId(), a.getType(), a.getBalance())
            );
        }
    }

    private void openNewAccount(Customer customer) {
        System.out.println("Select account type:");
        System.out.println("1. CHECKING");
        System.out.println("2. SAVINGS");
        System.out.println("0. Cancel");
        int typeChoice = getUserSelection(2);
        if (typeChoice == 0) return;

        AccountType accountType = typeChoice == 1 ? AccountType.CHECKING : AccountType.SAVINGS;

        System.out.print("Opening deposit amount: ");
        BigDecimal deposit = readAmount();
        if (deposit == null) return;

        try {
            Account account = accountService.createAdditionalAccount(customer.getId(), accountType, deposit);
            store.saveFullState(bank);
            System.out.println("Opened " + account.getType() + " account " + account.getId()
                    + " with balance " + account.getBalance() + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    private void deposit(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        System.out.print("Amount to deposit: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;
=======
    private void runTotalBalance(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for total-balance.");
            printUsage();
            return;
        }

        String customerId = args[1];

        try {
            BigDecimal totalBalance = accountService.getTotalBalance(customerId);
            System.out.println("Customer " + customerId + " total balance: " + totalBalance);
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
>>>>>>> main

        try {
            Account updated = accountService.depositIntoExistingAccount(accountId, amount);
            store.saveFullState(bank);
            System.out.println("Deposited " + amount + ". New balance: " + updated.getBalance() + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void withdraw(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        System.out.print("Amount to withdraw: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;

        try {
            Account updated = accountService.withdraw(accountId, amount);
            store.saveFullState(bank);
            System.out.println("Withdrew " + amount + ". New balance: " + updated.getBalance() + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    private void transfer(Customer customer) {
        System.out.println("Transfer FROM which account?");
        String fromId = promptAccountId(customer);
        if (fromId == null) return;

        System.out.print("Transfer TO (account ID): ");
        String toId = scanner.nextLine().trim();
        if (toId.isEmpty()) return;

        System.out.print("Amount to transfer: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;

        try {
            accountService.transfer(fromId, toId, amount);
=======
    private void runWithdraw(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for withdraw.");
            printUsage();
            return;
        }

        String accountId = args[1];
        String password = args[2];

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[3]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid withdraw amount: " + args[3]);
            return;
        }

        try {
            Account updatedAccount = accountService.withdraw(accountId, amount, password);
>>>>>>> main
            store.saveFullState(bank);
            System.out.println("Transferred " + amount + " from " + fromId + " to " + toId + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    private void transactionHistory(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        try {
            List<Transaction> history = accountService.getTransactionHistory(accountId);
            if (history.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }
            System.out.println();
            System.out.println("Transaction history for " + accountId + ":");
            for (Transaction t : history) {
                String related = t.getRelatedAccountId() != null
                        ? " | related: " + t.getRelatedAccountId() : "";
                System.out.printf("  %-12s  %-16s  amount: %-10s  balance after: %s%s%n",
                        t.getId(), t.getType(), t.getAmount(), t.getBalanceAfter(), related);
            }
=======
    private void runCloseAccount(String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid arguments for close-account.");
            printUsage();
            return;
        }

        String accountId = args[1];
        String password = args[2];

        try {
            BigDecimal cashOutAmount = accountService.closeAccount(accountId, password);
            store.saveFullState(bank);
            System.out.println("Closed account " + accountId + ". Cash-out amount: " + cashOutAmount);
>>>>>>> main
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    private void closeAccount(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;
=======
    private void runTransfer(String[] args) {
        if (args.length != 5) {
            System.out.println("Invalid arguments for transfer.");
            printUsage();
            return;
        }
        
        String password = args[4];
>>>>>>> main

        System.out.print("Are you sure you want to close account " + accountId + "? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("Cancelled.");
            return;
        }

        try {
<<<<<<< UI-reconstruct
            BigDecimal cashOut = accountService.closeAccount(accountId);
=======
            accountService.transfer(args[1], args[2], amount, password);
>>>>>>> main
            store.saveFullState(bank);
            System.out.println("Account " + accountId + " closed. Cash-out amount: " + cashOut + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    // ─── Admin Menu ───────────────────────────────────────────────────────────

    private void adminMenu(String username, String password) {
        while (true) {
            System.out.println();
            System.out.println("--- Admin Menu ---");
            System.out.println("1. View All Customers");
            System.out.println("2. View All Accounts");
            System.out.println("3. Collect Fee from Account");
            System.out.println("4. Add Interest to Account");
            System.out.println("5. Reset / Clear All Data");
            System.out.println("0. Logout");

            int selection = getUserSelection(5);
            switch (selection) {
                case 1: viewAllCustomers(); break;
                case 2: viewAllAccounts(); break;
                case 3: adminCollectFee(username, password); break;
                case 4: adminAddInterest(username, password); break;
                case 5: adminClearData(); break;
                case 0:
                    System.out.println("Logged out.");
                    return;
            }
        }
    }

    private void viewAllCustomers() {
        List<Customer> customers = bank.getCustomersSnapshot();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        System.out.println();
        System.out.println("All Customers:");
        for (Customer c : customers) {
            System.out.printf("  %-10s  %-20s  Accounts: %s%n",
                    c.getId(), c.getName(), c.getAccountIds());
        }
    }

    private void viewAllAccounts() {
        List<Account> accounts = bank.getAccountsSnapshot();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        System.out.println();
        System.out.println("All Accounts:");
        for (Account a : accounts) {
            System.out.printf("  %-10s  customer: %-10s  %-10s  balance: %s%n",
                    a.getId(), a.getCustomerId(), a.getType(), a.getBalance());
        }
    }

    private void adminCollectFee(String username, String password) {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        if (accountId.isEmpty()) return;

        System.out.print("Fee amount: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;

        try {
            Account updated = accountService.collectFee(username, password, accountId, amount);
            store.saveFullState(bank);
            System.out.println("Collected fee of " + amount + " from " + accountId
                    + ". New balance: " + updated.getBalance() + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void adminAddInterest(String username, String password) {
        System.out.print("Account ID: ");
        String accountId = scanner.nextLine().trim();
        if (accountId.isEmpty()) return;

        System.out.print("Interest amount: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;

        try {
            Account updated = accountService.addInterest(username, password, accountId, amount);
            store.saveFullState(bank);
            System.out.println("Added interest of " + amount + " to " + accountId
                    + ". New balance: " + updated.getBalance() + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    private void adminClearData() {
        System.out.print("This will reset ALL data. Type CONFIRM to proceed: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equals("CONFIRM")) {
            System.out.println("Cancelled.");
            return;
        }
=======
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

    private void runFreezeAccount(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for freeze-account.");
            printUsage();
            return;
        }

        try {
            Account updatedAccount = accountService.freezeAccount(args[1], args[2], args[3]);
            store.saveFullState(bank);
            System.out.println("Froze account " + updatedAccount.getId() + ".");
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runUnfreezeAccount(String[] args) {
        if (args.length != 4) {
            System.out.println("Invalid arguments for unfreeze-account.");
            printUsage();
            return;
        }

        try {
            Account updatedAccount = accountService.unfreezeAccount(args[1], args[2], args[3]);
            store.saveFullState(bank);
            System.out.println("Unfroze account " + updatedAccount.getId() + ".");
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void runViewInterestRate(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for view-interest-rate.");
            printUsage();
            return;
        }

        String accountId = args[1];

        try {
            BigDecimal interestRate = accountService.getInterestRate(accountId);
            System.out.println("Account " + accountId + " interest rate: " + interestRate);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void runListAccounts(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for list-accounts.");
            printUsage();
            return;
        }

        String customerId = args[1];
        try {
            List<Account> accounts = accountService.listAccounts(customerId);
            if (accounts.isEmpty()) {
                System.out.println("No accounts found for customer " + customerId);
                return;
            }
            System.out.println("Accounts for customer " + customerId + ":");
            for (Account account : accounts) {
                System.out.println(
                        "  " + account.getId()
                                + " | " + account.getType()
                                + " | balance: " + account.getBalance()
                );
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void runClearData() {
>>>>>>> main
        try {
            store.clearAllAndReseed();
            System.out.println("Data cleared and reseeded. Please restart the app.");
            System.exit(0);
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

<<<<<<< UI-reconstruct
    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int getUserSelection(int maxOption) {
        while (true) {
            System.out.print("Enter selection (0-" + maxOption + "): ");
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= 0 && val <= maxOption) return val;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Please enter a number between 0 and " + maxOption + ".");
        }
    }

    private String promptAccountId(Customer customer) {
        List<String> accountIds = customer.getAccountIds();
        if (accountIds.isEmpty()) {
            System.out.println("You have no accounts.");
            return null;
        }
        System.out.println("Your accounts:");
        for (int i = 0; i < accountIds.size(); i++) {
            String id = accountIds.get(i);
            int num = i + 1;
            bank.findAccount(id).ifPresent(a ->
                System.out.printf("  %d. %-10s  %-10s  Balance: %s%n",
                        num, a.getId(), a.getType(), a.getBalance())
            );
        }
        System.out.print("Enter account ID: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return null;
        if (!customer.getAccountIds().contains(input)) {
            System.out.println("Account not found or does not belong to you.");
            return null;
        }
        return input;
    }

    private BigDecimal readAmount() {
        String line = scanner.nextLine().trim();
        try {
            BigDecimal val = new BigDecimal(line);
            if (val.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be positive.");
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount.");
            return null;
        }
=======
    private void runListCustomers(String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid arguments for list-customers.");
            printUsage();
            return;
        }

        try {
            List<Customer> customers = accountService.listCustomers(args[1], args[2]);
            if (customers.isEmpty()) {
                System.out.println("No customers found.");
                return;
            }
            System.out.println("All customers:");
            for (Customer customer : customers) {
                System.out.println(
                        "  " + customer.getId()
                                + " | " + customer.getName()
                                + " | accounts: " + customer.getAccountIds().size()
                );
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
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
        System.out.println("  create-account <customerId> <password> <CHECKING|SAVINGS> <openingDeposit>");
        System.out.println("  transaction-history <accountId>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("  withdraw <accountId> <password> <amount>");
        System.out.println("  close-account <accountId> <password>");
        System.out.println("  transfer <fromAccountId> <toAccountId> <amount> <password>");
        System.out.println("  total-balance <customerId>");
        System.out.println("  collect-fee <adminUsername> <adminPassword> <accountId> <amount>");
        System.out.println("  add-interest <adminUsername> <adminPassword> <accountId> <amount>");
        System.out.println("  freeze-account <adminUsername> <adminPassword> <accountId>");
        System.out.println("  unfreeze-account <adminUsername> <adminPassword> <accountId>");
        System.out.println("  list-customers <adminUsername> <adminPassword>");
        System.out.println("  list-accounts <customerId>");
        System.out.println("  check-balance <accountId>");
        System.out.println("  clear-data");
        System.out.println("  set-interest-rate <adminUsername> <adminPassword> <accountId> <rate>");
        System.out.println("  view-interest-rate <accountId>");
        System.out.println("Examples:");
        System.out.println("  create-account CUST-001 password123 CHECKING 100.00");
        System.out.println("  check-balance ACC-0001");
        System.out.println("  total-balance CUST-001");
        System.out.println("  deposit ACC-0001 50.00");
        System.out.println("  withdraw ACC-0001 password123 25.00");
        System.out.println("  transaction-history ACC-0001");
        System.out.println("  transfer ACC-0001 ACC-0002 10.00 password123");
        System.out.println("  collect-fee admin admin123 ACC-0001 5.00");
        System.out.println("  add-interest admin admin123 ACC-0001 3.00");
        System.out.println("  set-interest-rate admin admin123 ACC-0001 0.05");
        System.out.println("  view-interest-rate ACC-0001");
        System.out.println("  list-customers <adminUsername> <adminPassword>");
        System.out.println("  list-accounts <customerId>");
        System.out.println("  freeze-account admin admin123 ACC-0001");
        System.out.println("  unfreeze-account admin admin123 ACC-0001");
>>>>>>> main
    }
}
