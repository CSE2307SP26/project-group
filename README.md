# project26

## Team Members

* Daniel Yan
* Bobby Yu
* Rosie Tang
* Nina Chen

---

# User Stories

1. A bank customer should be able to deposit into an existing account. (Shook)
2. A bank customer should be able to withdraw from an account.
3. A bank customer should be able to check their account balance.
4. A bank customer should be able to view their transaction history for an account.
5. A bank customer should be able to create an additional account with the bank.
6. A bank customer should be able to close an existing account.
7. A bank customer should be able to transfer money from one account to another.
8. A bank administrator should be able to collect fees from existing accounts when necessary.
9. A bank administrator should be able to add an interest payment to an existing account when necessary.

---

# What user stories do you intend to complete next iteration? (Iteration 1)

During **Iteration 1**, we will focus on implementing the core banking functionality that allows basic account operations.

Planned user stories:

* **#5** Create an additional account
* **#1** Deposit into an existing account
* **#2** Withdraw from an account
* **#3** Check account balance

Stretch goal (if time permits):

* **#4** View transaction history

These features will allow users to create accounts and perform basic financial operations.

---

# Is there anything that you implemented but doesn't currently work?

No known issues at this time for user story **#5**.

---

# What commands are needed to compile and run your code from the command line?

From the project root:

Windows (PowerShell):

* `.\gradlew.bat clean build`
* `.\gradlew.bat test`
* `.\gradlew.bat run --args="help"`
* `.\gradlew.bat run --args="create-account CUST-001 CHECKING 100.00"`
* `.\gradlew.bat run --args="check-balance ACC-0001"` (use the account id printed by `create-account`)
* `.\gradlew.bat run --args="clear-data"` (wipes the local database and re-seeds the demo customer `CUST-001`)

macOS/Linux:

* `./gradlew clean build`
* `./gradlew test`
* `./gradlew run --args="help"`
* `./gradlew run --args="create-account CUST-001 CHECKING 100.00"`
* `./gradlew run --args="check-balance ACC-0001"`
* `./gradlew run --args="clear-data"`

**Persistence:** Account data is stored in a local SQLite file named `bank.db` in the working directory (created on first run). Separate `gradlew run` invocations share this file, so balances survive between commands. To use a different path: add `-Dbank.db.file=/absolute/path/to/bank.db` to the `gradlew` command (before `run`).

Implemented in this iteration:

* User story **#5**: create an additional account for an existing customer
* User story **#3**: check an account balance via `check-balance <accountId>`
* Validation (unknown customer, invalid opening deposit, missing account) and unit tests
* Command-line commands: `create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>`, `check-balance <accountId>`, `clear-data`
* SQLite-backed storage so CLI runs persist state to disk
