package edu.washu.bank.service;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.AccountFrozenException;
import edu.washu.bank.exception.AccountNotFoundException;
import edu.washu.bank.exception.AuthenticationException;
import edu.washu.bank.exception.CustomerNotFoundException;
import edu.washu.bank.exception.InvalidDepositAmountException;
import edu.washu.bank.exception.InvalidOpeningDepositException;
import edu.washu.bank.exception.InvalidTransferException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.AdminUser;
import edu.washu.bank.model.Customer;
import edu.washu.bank.model.Transaction;
import edu.washu.bank.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {
    private static final String CUSTOMER_PASSWORD = "pass123";

    private Bank bank;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.addCustomer(new Customer("CUST-001", "Alice", CUSTOMER_PASSWORD));
        bank.addAdmin(new AdminUser("admin", "admin123"));
        accountService = new AccountService(bank);
    }

    @Test
    void createAdditionalAccountForExistingCustomerSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("250.00"),
                CUSTOMER_PASSWORD
        );

        assertEquals("CUST-001", account.getCustomerId());
        assertEquals(new BigDecimal("250.00"), account.getBalance());
        assertEquals(AccountType.CHECKING, account.getType());
        assertTrue(bank.findAccount(account.getId()).isPresent());
    }

    @Test
    void createAdditionalAccountAddsAccountToCustomerAndHistory() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("50.00"),
                CUSTOMER_PASSWORD
        );

        Customer customer = bank.findCustomer("CUST-001").orElseThrow();
        List<Transaction> history = accountService.getTransactionHistory(account.getId());

        assertEquals(1, customer.getAccountIds().size());
        assertEquals(account.getId(), customer.getAccountIds().get(0));
        assertEquals(1, history.size());
        assertEquals(TransactionType.ACCOUNT_OPENED, history.get(0).getType());
    }

    @Test
    void createAdditionalAccountForMissingCustomerThrows() {
        assertThrows(
                CustomerNotFoundException.class,
                () -> accountService.createAdditionalAccount("CUST-404", AccountType.CHECKING, BigDecimal.ZERO, "any")
        );
    }

    @Test
    void createAdditionalAccountWithWrongPasswordThrows() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, BigDecimal.ZERO, "wrong")
        );
    }

    @Test
    void createAdditionalAccountWithNegativeDepositThrows() {
        assertThrows(
                InvalidOpeningDepositException.class,
                () -> accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("-1.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void createMultipleAccountsUsesDeterministicSequentialIds() {
        Account first = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, BigDecimal.ZERO, CUSTOMER_PASSWORD);
        Account second = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, BigDecimal.ZERO, CUSTOMER_PASSWORD);

        assertEquals("ACC-0001", first.getId());
        assertEquals("ACC-0002", second.getId());
    }

    @Test
    void withdrawWithSufficientFundsSucceeds() {
        Account account = createCheckingAccount("100.00");

        Account updatedAccount = accountService.withdraw(account.getId(), new BigDecimal("30.00"), CUSTOMER_PASSWORD);

        assertEquals(new BigDecimal("70.00"), updatedAccount.getBalance());
        assertEquals(new BigDecimal("70.00"), bank.findAccount(account.getId()).orElseThrow().getBalance());
    }

    @Test
    void withdrawWithWrongPasswordThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                AuthenticationException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("30.00"), "wrong")
        );
    }

    @Test
    void withdrawWithInsufficientFundsThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("60.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void withdrawWithNegativeAmountThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("-10.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void withdrawWithInvalidAmountThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), null, CUSTOMER_PASSWORD)
        );
    }

    @Test
    void withdrawFromNonexistentAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.withdraw("ACC-404", new BigDecimal("10.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void getBalanceForExistingAccountReturnsCorrectBalance() {
        Account account = createCheckingAccount("500.00");

        BigDecimal balance = accountService.getBalance(account.getId());

        assertEquals(new BigDecimal("500.00"), balance);
    }

    @Test
    void getBalanceForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getBalance("ACC-9999")
        );
    }

    @Test
    void getTotalBalanceSumsBalancesAcrossAllCustomerAccounts() {
        createCheckingAccount("500.00");
        accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("125.50"), CUSTOMER_PASSWORD);

        BigDecimal totalBalance = accountService.getTotalBalance("CUST-001");

        assertEquals(new BigDecimal("625.50"), totalBalance);
    }

    @Test
    void getTotalBalanceReflectsBalanceChangingOperations() {
        Account checking = createCheckingAccount("100.00");
        Account savings = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"), CUSTOMER_PASSWORD);

        accountService.depositIntoExistingAccount(checking.getId(), new BigDecimal("10.00"));
        accountService.withdraw(savings.getId(), new BigDecimal("5.00"), CUSTOMER_PASSWORD);

        BigDecimal totalBalance = accountService.getTotalBalance("CUST-001");

        assertEquals(new BigDecimal("145.00"), totalBalance);
    }

    @Test
    void getTotalBalanceForMissingCustomerThrows() {
        assertThrows(
                CustomerNotFoundException.class,
                () -> accountService.getTotalBalance("CUST-404")
        );
    }

    @Test
    void depositIntoExistingAccountSucceeds() {
        Account account = createCheckingAccount("100.00");

        Account updatedAccount = accountService.depositIntoExistingAccount(
                account.getId(),
                new BigDecimal("50.00")
        );

        assertEquals(new BigDecimal("150.00"), updatedAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), bank.findAccount(account.getId()).orElseThrow().getBalance());
    }

    @Test
    void depositIntoMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.depositIntoExistingAccount("ACC-9999", new BigDecimal("50.00"))
        );
    }

    @Test
    void depositWithZeroAmountThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                InvalidDepositAmountException.class,
                () -> accountService.depositIntoExistingAccount(account.getId(), BigDecimal.ZERO)
        );
    }

    @Test
    void depositWithNegativeAmountThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                InvalidDepositAmountException.class,
                () -> accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("-10.00"))
        );
    }

    @Test
    void transactionHistoryIncludesDepositsAndWithdrawals() {
        Account account = createCheckingAccount("100.00");

        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("25.00"));
        accountService.withdraw(account.getId(), new BigDecimal("10.00"), CUSTOMER_PASSWORD);

        List<Transaction> history = accountService.getTransactionHistory(account.getId());

        assertEquals(3, history.size());
        assertEquals(TransactionType.ACCOUNT_OPENED, history.get(0).getType());
        assertEquals(TransactionType.DEPOSIT, history.get(1).getType());
        assertEquals(TransactionType.WITHDRAWAL, history.get(2).getType());
    }

    @Test
    void getTransactionHistoryByTypeReturnsOnlyMatchingTransactions() {
        Account account = createCheckingAccount("100.00");

        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("25.00"));
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("5.00"));
        accountService.withdraw(account.getId(), new BigDecimal("10.00"), CUSTOMER_PASSWORD);

        List<Transaction> history = accountService.getTransactionHistory(account.getId(), TransactionType.DEPOSIT);

        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(transaction -> transaction.getType() == TransactionType.DEPOSIT));
    }

    @Test
    void getTransactionHistoryByTypeReturnsEmptyListWhenAccountHasNoMatches() {
        Account account = createCheckingAccount("100.00");

        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("25.00"));

        List<Transaction> history = accountService.getTransactionHistory(account.getId(), TransactionType.FEE);

        assertTrue(history.isEmpty());
    }

    @Test
    void getTransactionHistoryForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getTransactionHistory("ACC-404")
        );
    }

    @Test
    void getTransactionHistoryByTypeForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getTransactionHistory("ACC-404", TransactionType.DEPOSIT)
        );
    }

    @Test
    void closeAccountRemovesItAndKeepsHistory() {
        Account account = createCheckingAccount("120.00");

        BigDecimal cashOutAmount = accountService.closeAccount(account.getId(), CUSTOMER_PASSWORD);

        assertEquals(new BigDecimal("120.00"), cashOutAmount);
        assertTrue(bank.findAccount(account.getId()).isEmpty());
        assertTrue(bank.findCustomer("CUST-001").orElseThrow().getAccountIds().isEmpty());
        assertEquals(2, accountService.getTransactionHistory(account.getId()).size());
        assertEquals(
                TransactionType.ACCOUNT_CLOSED,
                accountService.getTransactionHistory(account.getId()).get(1).getType()
        );
    }

    @Test
    void closeAccountWithWrongPasswordThrows() {
        Account account = createCheckingAccount("120.00");

        assertThrows(
                AuthenticationException.class,
                () -> accountService.closeAccount(account.getId(), "wrong")
        );
    }

    @Test
    void transferMovesMoneyAndRecordsBothSides() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"), CUSTOMER_PASSWORD);

        accountService.transfer(source.getId(), target.getId(), new BigDecimal("30.00"), CUSTOMER_PASSWORD);

        assertEquals(new BigDecimal("70.00"), bank.findAccount(source.getId()).orElseThrow().getBalance());
        assertEquals(new BigDecimal("70.00"), bank.findAccount(target.getId()).orElseThrow().getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, lastTransaction(source.getId()).getType());
        assertEquals(TransactionType.TRANSFER_IN, lastTransaction(target.getId()).getType());
        assertEquals(target.getId(), lastTransaction(source.getId()).getRelatedAccountId());
    }

    @Test
    void transferToSameAccountThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.transfer(account.getId(), account.getId(), BigDecimal.ONE, CUSTOMER_PASSWORD)
        );
    }

    @Test
    void transferWithWrongPasswordThrows() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"), CUSTOMER_PASSWORD);

        assertThrows(
                AuthenticationException.class,
                () -> accountService.transfer(source.getId(), target.getId(), new BigDecimal("10.00"), "wrong")
        );
    }

    @Test
    void collectFeeRequiresValidAdminCredentials() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                AuthenticationException.class,
                () -> accountService.collectFee("admin", "wrong", account.getId(), new BigDecimal("5.00"))
        );
    }

    @Test
    void collectFeeDebitsAccountAndRecordsHistory() {
        Account account = createCheckingAccount("100.00");

        Account updatedAccount = accountService.collectFee("admin", "admin123", account.getId(), new BigDecimal("5.00"));

        assertEquals(new BigDecimal("95.00"), updatedAccount.getBalance());
        assertEquals(TransactionType.FEE, lastTransaction(account.getId()).getType());
    }

    @Test
    void freezeAccountRequiresValidAdminCredentials() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                AuthenticationException.class,
                () -> accountService.freezeAccount("admin", "wrong", account.getId())
        );
    }

    @Test
    void freezeAndUnfreezeAccountUpdatesFrozenState() {
        Account account = createCheckingAccount("100.00");

        Account frozenAccount = accountService.freezeAccount("admin", "admin123", account.getId());
        Account unfrozenAccount = accountService.unfreezeAccount("admin", "admin123", account.getId());

        assertTrue(frozenAccount.isFrozen());
        assertFalse(unfrozenAccount.isFrozen());
        assertFalse(bank.findAccount(account.getId()).orElseThrow().isFrozen());
    }

    @Test
    void depositIntoFrozenAccountThrows() {
        Account account = createCheckingAccount("100.00");
        accountService.freezeAccount("admin", "admin123", account.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("10.00"))
        );
    }

    @Test
    void withdrawFromFrozenAccountThrows() {
        Account account = createCheckingAccount("100.00");
        accountService.freezeAccount("admin", "admin123", account.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("10.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void transferFromFrozenAccountThrows() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"), CUSTOMER_PASSWORD);
        accountService.freezeAccount("admin", "admin123", source.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.transfer(source.getId(), target.getId(), new BigDecimal("10.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void transferIntoFrozenAccountThrows() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"), CUSTOMER_PASSWORD);
        accountService.freezeAccount("admin", "admin123", target.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.transfer(source.getId(), target.getId(), new BigDecimal("10.00"), CUSTOMER_PASSWORD)
        );
    }

    @Test
    void collectFeeFromFrozenAccountThrows() {
        Account account = createCheckingAccount("100.00");
        accountService.freezeAccount("admin", "admin123", account.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.collectFee("admin", "admin123", account.getId(), new BigDecimal("5.00"))
        );
    }

    @Test
    void addInterestCreditsAccountAndRecordsHistory() {
        Account account = createCheckingAccount("100.00");

        Account updatedAccount = accountService.addInterest("admin", "admin123", account.getId(), new BigDecimal("3.00"));

        assertEquals(new BigDecimal("103.00"), updatedAccount.getBalance());
        assertEquals(TransactionType.INTEREST, lastTransaction(account.getId()).getType());
    }

    @Test
    void setInterestRateForSavingsAccountSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                CUSTOMER_PASSWORD
        );

        Account updatedAccount = accountService.setInterestRate(
                "admin",
                "admin123",
                account.getId(),
                new BigDecimal("0.05")
        );

        assertEquals(new BigDecimal("0.05"), updatedAccount.getInterestRate());
        assertEquals(
                new BigDecimal("0.05"),
                bank.findAccount(account.getId()).orElseThrow().getInterestRate()
        );
    }

    @Test
    void setInterestRateWithInvalidAdminCredentialsThrows() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                CUSTOMER_PASSWORD
        );

        assertThrows(
                AuthenticationException.class,
                () -> accountService.setInterestRate(
                        "admin",
                        "wrong-password",
                        account.getId(),
                        new BigDecimal("0.05")
                )
        );
    }

    @Test
    void listCustomersWithValidAdminReturnsAllCustomers() {
        List<Customer> customers = accountService.listCustomers("admin", "admin123");
        assertEquals(1, customers.size());
        assertEquals("CUST-001", customers.get(0).getId());
    }

    @Test
    void listCustomersWithInvalidAdminThrows() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.listCustomers("admin", "wrongpassword")
        );
    }

    @Test
    void listFrozenAccountsWithValidAdminReturnsOnlyFrozenAccounts() {
        Account frozenChecking = createCheckingAccount("100.00");
        Account activeSavings = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("50.00"),
                CUSTOMER_PASSWORD
        );
        accountService.freezeAccount("admin", "admin123", frozenChecking.getId());

        List<Account> frozenAccounts = accountService.listFrozenAccounts("admin", "admin123");

        assertEquals(1, frozenAccounts.size());
        assertEquals(frozenChecking.getId(), frozenAccounts.get(0).getId());
        assertTrue(frozenAccounts.stream().allMatch(Account::isFrozen));
        assertFalse(frozenAccounts.stream().anyMatch(account -> account.getId().equals(activeSavings.getId())));
    }

    @Test
    void listFrozenAccountsWithInvalidAdminThrows() {
        assertThrows(
                AuthenticationException.class,
                () -> accountService.listFrozenAccounts("admin", "wrongpassword")
        );
    }

    @Test
    void listFrozenAccountsReturnsEmptyWhenNoAccountsAreFrozen() {
        createCheckingAccount("100.00");

        List<Account> frozenAccounts = accountService.listFrozenAccounts("admin", "admin123");

        assertTrue(frozenAccounts.isEmpty());
    }

    @Test
    void setInterestRateForCheckingAccountThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.setInterestRate(
                        "admin",
                        "admin123",
                        account.getId(),
                        new BigDecimal("0.05")
                )
        );
    }

    @Test
    void setInterestRateWithNegativeRateThrows() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                CUSTOMER_PASSWORD
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.setInterestRate(
                        "admin",
                        "admin123",
                        account.getId(),
                        new BigDecimal("-0.01")
                )
        );
    }

    @Test
    void getInterestRateForSavingsAccountSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.SAVINGS,
                new BigDecimal("100.00"),
                CUSTOMER_PASSWORD
        );

        accountService.setInterestRate(
                "admin",
                "admin123",
                account.getId(),
                new BigDecimal("0.05")
        );

        BigDecimal interestRate = accountService.getInterestRate(account.getId());

        assertEquals(new BigDecimal("0.05"), interestRate);
    }

    @Test
    void getInterestRateForCheckingAccountThrows() {
        Account account = createCheckingAccount("100.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getInterestRate(account.getId())
        );
    }

    @Test
    void getInterestRateForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getInterestRate("ACC-404")
        );
    }

    @Test
    void listAccountsForExistingCustomerReturnsAllAccounts() {
        accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("100.00"), CUSTOMER_PASSWORD);
        accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("50.00"), CUSTOMER_PASSWORD);

        List<Account> accounts = accountService.listAccounts("CUST-001");

        assertEquals(2, accounts.size());
    }

    @Test
    void listAccountsForMissingCustomerThrows() {
        assertThrows(
                CustomerNotFoundException.class,
                () -> accountService.listAccounts("CUST-404")
        );
    }

    @Test
    void addInterestToFrozenAccountThrows() {
        Account account = createCheckingAccount("100.00");
        accountService.freezeAccount("admin", "admin123", account.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.addInterest("admin", "admin123", account.getId(), new BigDecimal("3.00"))
        );
    }

    @Test
    void operationsResumeAfterAccountIsUnfrozen() {
        Account account = createCheckingAccount("100.00");
        accountService.freezeAccount("admin", "admin123", account.getId());
        accountService.unfreezeAccount("admin", "admin123", account.getId());

        Account updatedAccount = accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("15.00"));

        assertEquals(new BigDecimal("115.00"), updatedAccount.getBalance());
    }

    @Test
    void getTransactionsSortedByAmountReturnsSortedDescending() {
        Account account = createCheckingAccount("200.00");
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("50.00"));
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("10.00"));
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("30.00"));

        List<Transaction> sorted = accountService.getTransactionsSortedByAmount(account.getId());

        assertTrue(sorted.get(0).getAmount().compareTo(sorted.get(1).getAmount()) >= 0);
        assertTrue(sorted.get(1).getAmount().compareTo(sorted.get(2).getAmount()) >= 0);
    }

    @Test
    void getRecentTransactionsReturnsLastNTransactions() {
        Account account = createCheckingAccount("100.00");
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("10.00"));
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("20.00"));
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("30.00"));

        List<Transaction> recent = accountService.getRecentTransactions(account.getId(), 2);

        assertEquals(2, recent.size());
        assertEquals(new BigDecimal("20.00"), recent.get(0).getAmount());
        assertEquals(new BigDecimal("30.00"), recent.get(1).getAmount());
    }

    @Test
    void getRecentTransactionsWhenNExceedsTotalReturnsAll() {
        Account account = createCheckingAccount("100.00");
        accountService.depositIntoExistingAccount(account.getId(), new BigDecimal("10.00"));

        List<Transaction> recent = accountService.getRecentTransactions(account.getId(), 10);

        assertEquals(2, recent.size());
    }

    private Account createCheckingAccount(String openingBalance) {
        return accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal(openingBalance),
                CUSTOMER_PASSWORD
        );
    }

    private Transaction lastTransaction(String accountId) {
        List<Transaction> history = accountService.getTransactionHistory(accountId);
        return history.get(history.size() - 1);
    }
}