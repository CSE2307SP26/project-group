# project26

## Team Members

* Daniel Yan
* Bobby Yu
* Rosie Tang
* Nina Chen

---

# Features planned to implement this iteration (iteration 2)

* **#8** Close an existing account
* **#9** Customer can transfer money from one account to another
* **#10** Bank admin can collect fees from existing account
* **#11** Bank admin can add interest to existing account
* **#17** Admin login
* **#18** Overdraft protection
* **#19** Files to store user story info
* **#20** Password protection

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

macOS/Linux:

* `./gradlew clean build`
* `./gradlew test`
* `./gradlew run --args="help"`
* `./gradlew run --args="create-account CUST-001 CHECKING 100.00"`

Implemented in this iteration:

* User story **#5**: create an additional account for an existing customer
* Includes validation (unknown customer and invalid opening deposit) and unit tests
* Command-line support for account creation via `create-account <customerId> <CHECKING|SAVINGS> <openingDeposit>`
