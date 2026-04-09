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
    private Bank bank;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        bank = new Bank();
        bank.addCustomer(new Customer("CUST-001", "Alice"));
        bank.addAdmin(new AdminUser("admin", "admin123"));
        accountService = new AccountService(bank);
    }

    @Test
    void createAdditionalAccountForExistingCustomerSucceeds() {
        Account account = accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal("250.00")
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
                new BigDecimal("50.00")
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
                () -> accountService.createAdditionalAccount("CUST-404", AccountType.CHECKING, BigDecimal.ZERO)
        );
    }

    @Test
    void createAdditionalAccountWithNegativeDepositThrows() {
        assertThrows(
                InvalidOpeningDepositException.class,
                () -> accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, new BigDecimal("-1.00"))
        );
    }

    @Test
    void createMultipleAccountsUsesDeterministicSequentialIds() {
        Account first = accountService.createAdditionalAccount("CUST-001", AccountType.CHECKING, BigDecimal.ZERO);
        Account second = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, BigDecimal.ZERO);

        assertEquals("ACC-0001", first.getId());
        assertEquals("ACC-0002", second.getId());
    }

    @Test
    void withdrawWithSufficientFundsSucceeds() {
        Account account = createCheckingAccount("100.00");

        Account updatedAccount = accountService.withdraw(account.getId(), new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00"), updatedAccount.getBalance());
        assertEquals(new BigDecimal("70.00"), bank.findAccount(account.getId()).orElseThrow().getBalance());
    }

    @Test
    void withdrawWithInsufficientFundsThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("60.00"))
        );
    }

    @Test
    void withdrawWithNegativeAmountThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), new BigDecimal("-10.00"))
        );
    }

    @Test
    void withdrawWithInvalidAmountThrows() {
        Account account = createCheckingAccount("50.00");

        assertThrows(
                InvalidTransferException.class,
                () -> accountService.withdraw(account.getId(), null)
        );
    }

    @Test
    void withdrawFromNonexistentAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.withdraw("ACC-404", new BigDecimal("10.00"))
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
        accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("125.50"));

        BigDecimal totalBalance = accountService.getTotalBalance("CUST-001");

        assertEquals(new BigDecimal("625.50"), totalBalance);
    }

    @Test
    void getTotalBalanceReflectsBalanceChangingOperations() {
        Account checking = createCheckingAccount("100.00");
        Account savings = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"));

        accountService.depositIntoExistingAccount(checking.getId(), new BigDecimal("10.00"));
        accountService.withdraw(savings.getId(), new BigDecimal("5.00"));

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
        accountService.withdraw(account.getId(), new BigDecimal("10.00"));

        List<Transaction> history = accountService.getTransactionHistory(account.getId());

        assertEquals(3, history.size());
        assertEquals(TransactionType.ACCOUNT_OPENED, history.get(0).getType());
        assertEquals(TransactionType.DEPOSIT, history.get(1).getType());
        assertEquals(TransactionType.WITHDRAWAL, history.get(2).getType());
    }

    @Test
    void getTransactionHistoryForMissingAccountThrows() {
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getTransactionHistory("ACC-404")
        );
    }

    @Test
    void closeAccountRemovesItAndKeepsHistory() {
        Account account = createCheckingAccount("120.00");

        BigDecimal cashOutAmount = accountService.closeAccount(account.getId());

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
    void transferMovesMoneyAndRecordsBothSides() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"));

        accountService.transfer(source.getId(), target.getId(), new BigDecimal("30.00"));

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
                () -> accountService.transfer(account.getId(), account.getId(), BigDecimal.ONE)
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
                () -> accountService.withdraw(account.getId(), new BigDecimal("10.00"))
        );
    }

    @Test
    void transferFromFrozenAccountThrows() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"));
        accountService.freezeAccount("admin", "admin123", source.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.transfer(source.getId(), target.getId(), new BigDecimal("10.00"))
        );
    }

    @Test
    void transferIntoFrozenAccountThrows() {
        Account source = createCheckingAccount("100.00");
        Account target = accountService.createAdditionalAccount("CUST-001", AccountType.SAVINGS, new BigDecimal("40.00"));
        accountService.freezeAccount("admin", "admin123", target.getId());

        assertThrows(
                AccountFrozenException.class,
                () -> accountService.transfer(source.getId(), target.getId(), new BigDecimal("10.00"))
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

    private Account createCheckingAccount(String openingBalance) {
        return accountService.createAdditionalAccount(
                "CUST-001",
                AccountType.CHECKING,
                new BigDecimal(openingBalance)
        );
    }

    private Transaction lastTransaction(String accountId) {
        List<Transaction> history = accountService.getTransactionHistory(accountId);
        return history.get(history.size() - 1);
    }
}
