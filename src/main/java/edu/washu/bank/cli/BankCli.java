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
            System.out.println("8. Set Balance Alert Threshold");
            System.out.println("0. Logout");

            int selection = getUserSelection(8);
            switch (selection) {
                case 1: viewAccounts(customer); break;
                case 2: openNewAccount(customer); break;
                case 3: deposit(customer); break;
                case 4: withdraw(customer); break;
                case 5: transfer(customer); break;
                case 6: transactionHistory(customer); break;
                case 7: closeAccount(customer); break;
                case 8: setBalanceAlert(customer); break;
                case 0:
                    System.out.println("Logged out.");
                    return;
            }

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

    private void deposit(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        System.out.print("Amount to deposit: ");
        BigDecimal amount = readAmount();
        if (amount == null) return;

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
            store.saveFullState(bank);
            System.out.println("Transferred " + amount + " from " + fromId + " to " + toId + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

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

        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void closeAccount(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        System.out.print("Are you sure you want to close account " + accountId + "? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("Cancelled.");
            return;
        }
        try {
            BigDecimal cashOut = accountService.closeAccount(accountId);
            store.saveFullState(bank);
            System.out.println("Account " + accountId + " closed. Cash-out amount: " + cashOut + ".");
        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

    private void setBalanceAlert(Customer customer) {
        String accountId = promptAccountId(customer);
        if (accountId == null) return;

        Account account = bank.findAccount(accountId).orElse(null);
        if (account == null) return;

        System.out.println("Current alert threshold: " + account.getAlertBalanceThreshold());
        System.out.print("Enter new alert threshold amount: ");
        BigDecimal newThreshold = readAmount();
        if (newThreshold == null) return;

        try {
            Account updatedAccount = account.withAlertBalanceThreshold(newThreshold);
            bank.saveAccount(updatedAccount);
            store.saveFullState(bank);
            System.out.println("Alert threshold for " + accountId + " updated to " + newThreshold + ".");
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

    private void adminClearData() {
        System.out.print("This will reset ALL data. Type CONFIRM to proceed: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equals("CONFIRM")) {
            System.out.println("Cancelled.");
            return;
        }
        try {
            store.clearAllAndReseed();
            System.out.println("Data cleared and reseeded. Please restart the app.");
            System.exit(0);
        } catch (SQLException ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
    }

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
        System.out.print("Enter account ID or number (e.g., 1 or ACC-0001): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return null;

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < accountIds.size()) {
                return accountIds.get(index);
            }
        } catch (NumberFormatException ignored) {}

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
    }
}