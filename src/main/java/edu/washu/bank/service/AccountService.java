package edu.washu.bank.service;

import edu.washu.bank.core.Bank;
import edu.washu.bank.exception.AccountFrozenException;
import edu.washu.bank.exception.AccountNotFoundException;
import edu.washu.bank.exception.AuthenticationException;
import edu.washu.bank.exception.CustomerNotFoundException;
import edu.washu.bank.exception.InvalidOpeningDepositException;
import edu.washu.bank.exception.InvalidTransferException;
import edu.washu.bank.model.Account;
import edu.washu.bank.model.AccountType;
import edu.washu.bank.model.AdminUser;
import edu.washu.bank.model.Customer;
import edu.washu.bank.model.Transaction;
import edu.washu.bank.model.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class AccountService {
    private final Bank bank;

    public AccountService(Bank bank) {
        this.bank = Objects.requireNonNull(bank, "bank must not be null");
    }

    public Account createAdditionalAccount(String customerId, AccountType accountType, BigDecimal openingDeposit, String password) {
        if (openingDeposit == null || openingDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOpeningDepositException("Opening deposit must be at least 0");
        }

        Customer customer = bank.findCustomer(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (!customer.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid customer credentials.");
        }

        String accountId = bank.nextAccountId();
        Account account = new Account(accountId, customer.getId(), accountType, openingDeposit);
        bank.saveAccount(account);
        customer.addAccountId(accountId);
        recordTransaction(
                accountId,
                TransactionType.ACCOUNT_OPENED,
                openingDeposit,
                openingDeposit,
                null,
                "Account opened"
        );
        return account;
    }

    public Account depositIntoExistingAccount(String accountId, BigDecimal amount) {
        Account existingAccount = requireAccount(accountId);
        ensureAccountIsNotFrozen(existingAccount, "deposit into");
        Account updatedAccount = existingAccount.deposit(amount);
        bank.saveAccount(updatedAccount);
        recordTransaction(
                accountId,
                TransactionType.DEPOSIT,
                amount,
                updatedAccount.getBalance(),
                null,
                "Deposit"
        );
        return updatedAccount;
    }

    public Account withdraw(String accountId, BigDecimal amount, String password) {
        authenticateCustomer(accountId, password);

        Account account = requireAccount(accountId);
        ensureAccountIsNotFrozen(account, "withdraw from");
        Account updatedAccount = account.withdraw(amount);
        bank.saveAccount(updatedAccount);
        recordTransaction(
                accountId,
                TransactionType.WITHDRAWAL,
                amount,
                updatedAccount.getBalance(),
                null,
                "Withdrawal"
        );
        return updatedAccount;
    }

    public BigDecimal getBalance(String accountId) {
        return requireAccount(accountId).getBalance();
    }

    public BigDecimal getTotalBalance(String customerId) {
        Customer customer = bank.findCustomer(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        BigDecimal totalBalance = BigDecimal.ZERO;
        for (String accountId : customer.getAccountIds()) {
            totalBalance = totalBalance.add(requireAccount(accountId).getBalance());
        }
        return totalBalance;
    }

    public List<Transaction> getTransactionHistory(String accountId) {
        List<Transaction> history = bank.findTransactionsForAccount(accountId);
        if (history.isEmpty() && bank.findAccount(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        return history;
    }

    public BigDecimal closeAccount(String accountId, String password) {
        authenticateCustomer(accountId, password);

        Account account = requireAccount(accountId);
        BigDecimal closingBalance = account.getBalance();
        recordTransaction(
                accountId,
                TransactionType.ACCOUNT_CLOSED,
                closingBalance,
                BigDecimal.ZERO,
                null,
                "Account closed"
        );
        bank.removeAccount(accountId);
        bank.findCustomer(account.getCustomerId()).ifPresent(customer -> customer.removeAccountId(accountId));
        return closingBalance;
    }

    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount, String password) {
        authenticateCustomer(fromAccountId, password);
        
        if (Objects.equals(fromAccountId, toAccountId)) {
            throw new InvalidTransferException("Cannot transfer to the same account.");
        }

        Account fromAccount = requireAccount(fromAccountId);
        Account toAccount = requireAccount(toAccountId);
        ensureAccountIsNotFrozen(fromAccount, "transfer from");
        ensureAccountIsNotFrozen(toAccount, "transfer into");

        Account updatedFrom = fromAccount.withdraw(amount);
        Account updatedTo = toAccount.deposit(amount);
        bank.saveAccount(updatedFrom);
        bank.saveAccount(updatedTo);
        recordTransaction(
                fromAccountId,
                TransactionType.TRANSFER_OUT,
                amount,
                updatedFrom.getBalance(),
                toAccountId,
                "Transfer to " + toAccountId
        );
        recordTransaction(
                toAccountId,
                TransactionType.TRANSFER_IN,
                amount,
                updatedTo.getBalance(),
                fromAccountId,
                "Transfer from " + fromAccountId
        );
    }

    public Account collectFee(String username, String password, String accountId, BigDecimal amount) {
        authenticateAdmin(username, password);
        Account account = requireAccount(accountId);
        ensureAccountIsNotFrozen(account, "withdraw from");
        Account updatedAccount = account.withdraw(amount);
        bank.saveAccount(updatedAccount);
        recordTransaction(
                accountId,
                TransactionType.FEE,
                amount,
                updatedAccount.getBalance(),
                null,
                "Administrative fee"
        );
        return updatedAccount;
    }

    public Account addInterest(String username, String password, String accountId, BigDecimal amount) {
        authenticateAdmin(username, password);
        Account account = requireAccount(accountId);
        ensureAccountIsNotFrozen(account, "deposit into");
        Account updatedAccount = account.deposit(amount);
        bank.saveAccount(updatedAccount);
        recordTransaction(
                accountId,
                TransactionType.INTEREST,
                amount,
                updatedAccount.getBalance(),
                null,
                "Interest payment"
        );
        return updatedAccount;
    }

    public Account setInterestRate(String username, String password, String accountId, BigDecimal interestRate) {
        authenticateAdmin(username, password);

        Account account = requireAccount(accountId);
        Account updatedAccount = account.withInterestRate(interestRate);

        bank.saveAccount(updatedAccount);
        return updatedAccount;
    }

    public BigDecimal getInterestRate(String accountId) {
        Account account = requireAccount(accountId);

        if (account.getType() != AccountType.SAVINGS) {
            throw new IllegalArgumentException("Interest rates are only available for savings accounts.");
        }

        return account.getInterestRate();
    }

    public Account freezeAccount(String username, String password, String accountId) {
        authenticateAdmin(username, password);
        Account account = requireAccount(accountId);
        Account updatedAccount = account.freeze();
        bank.saveAccount(updatedAccount);
        return updatedAccount;
    }

    public Account unfreezeAccount(String username, String password, String accountId) {
        authenticateAdmin(username, password);
        Account account = requireAccount(accountId);
        Account updatedAccount = account.unfreeze();
        bank.saveAccount(updatedAccount);
        return updatedAccount;
    }

    private void authenticateAdmin(String username, String password) {
        AdminUser adminUser = bank.findAdmin(username)
                .orElseThrow(() -> new AuthenticationException("Invalid admin credentials."));
        if (!adminUser.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid admin credentials.");
        }
    }

    private void authenticateCustomer(String accountId, String password) {
        Account account = requireAccount(accountId);
        Customer customer = bank.findCustomer(account.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(account.getCustomerId()));
        if (!customer.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid customer credentials.");
        }
    }

    private Account requireAccount(String accountId) {
        return bank.findAccount(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private void ensureAccountIsNotFrozen(Account account, String action) {
        if (account.isFrozen()) {
            throw new AccountFrozenException(
                    "Account " + account.getId() + " is frozen. Cannot " + action + " this account."
            );
        }
    }

    private void recordTransaction(
            String accountId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String relatedAccountId,
            String description
    ) {
        bank.addTransaction(new Transaction(
                bank.nextTransactionId(),
                accountId,
                type,
                amount,
                balanceAfter,
                relatedAccountId,
                description
        ));
    }

    public List<Customer> listCustomers(String username, String password) {
        authenticateAdmin(username, password);
        return bank.getCustomersSnapshot();
    }

    public List<Account> listAccounts(String customerId) {
        Customer customer = bank.findCustomer(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        return customer.getAccountIds().stream()
                .map(id -> bank.findAccount(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(java.util.stream.Collectors.toList());
    }
}