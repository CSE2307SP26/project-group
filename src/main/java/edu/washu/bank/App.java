package edu.washu.bank;

import edu.washu.bank.core.Bank;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.Customer;
import edu.washu.bank.service.AccountService;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        Bank bank = new Bank();
        bank.addCustomer(new Customer("CUST-001", "Demo User"));
        AccountService accountService = new AccountService(bank);
        accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("100.00"));

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printUsage();
            return;
        }

        if ("create-account".equalsIgnoreCase(args[0])) {
            runCreateAccount(accountService, args);
            return;
        }

        if ("check-balance".equalsIgnoreCase(args[0])) {
            runCheckBalance(accountService, args);
            return;
        }

        if ("deposit".equalsIgnoreCase(args[0])) {
            runDeposit(accountService, args);
            return;
        }

        System.out.println("Unknown command: " + args[0]);
        printUsage();
    }

    private static void runCreateAccount(AccountService accountService, String[] args) {
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
            System.out.println(
                    "Created account " + account.getId()
                            + " (" + account.getType() + ") for customer " + account.getCustomerId()
                            + " with opening balance " + account.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void runCheckBalance(AccountService accountService, String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid arguments for check-balance.");
            printUsage();
            return;
        }

        String accountId = args[1];

        try {
            java.math.BigDecimal balance = accountService.getBalance(accountId);
            System.out.println("Account " + accountId + " balance: " + balance);
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void runDeposit(AccountService accountService, String[] args) {
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
            System.out.println(
                    "Deposited " + amount
                            + " into account " + updatedAccount.getId()
                            + ". New balance: " + updatedAccount.getBalance()
            );
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void printUsage() {
        System.out.println("Bank CLI");
        System.out.println("Seeded customer for demo: CUST-001");
        System.out.println("Seeded account for demo: ACC-0001 (CHECKING, balance 100.00)");
        System.out.println("Usage:");
        System.out.println("  create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>");
        System.out.println("  check-balance <accountId>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("Examples:");
        System.out.println("  create-account CUST-001 CHECKING 100.00");
        System.out.println("  check-balance ACC-0001");
        System.out.println("  deposit ACC-0001 50.00");
    }
}
