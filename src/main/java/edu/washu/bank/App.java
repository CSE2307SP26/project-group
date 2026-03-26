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

        // If args provided, run once (for gradle run --args)
        if (args.length > 0) {
            runSingleCommand(accountService, args);
            return;
        }

        // Otherwise, go to interactive mode, can run multiple commands
        runInteractive(accountService);
    }

    private static void runInteractive(AccountService accountService) {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        System.out.println("Bank CLI (type 'exit' to quit)");
        printUsage();

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();

            if (line.equalsIgnoreCase("exit")) {
                break;
            }

            if (!scanner.hasNextLine()) {
                System.out.println("\nNo input available. Exiting...");
                break;
            }

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] args = line.split("\\s+");
            runSingleCommand(accountService, args);
        }

        scanner.close();
    }

    private static void runSingleCommand(AccountService accountService, String[] args) {
        if (args.length == 0) {
            System.out.println("No command provided.");
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "create-account":
                runCreateAccount(accountService, args);
                break;
            case "withdraw":
                runWithdraw(accountService, args);
                break;
            default:
                System.out.println("Unknown command: " + command);
                printUsage();
        }
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

    private static void printUsage() {
        System.out.println("Bank CLI");
        System.out.println("Seeded customer for demo: CUST-001");
        System.out.println("Usage:");
        System.out.println("  create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>");
        System.out.println("  deposit <accountId> <amount>");
        System.out.println("  withdraw <accountId> <amount>");
        System.out.println("Example:");
        System.out.println("  create-account CUST-001 CHECKING 100.00");
        System.out.println("  withdraw ACC-001 25.00");
    }
}
