# project26

## Team Members

* Daniel Yan
* Bobby Yu
* Rosie Tang
* Nina Chen

---

# Features planned to implement this iteration (iteration 2)

10. A bank administrator should be able to freeze and unfreeze an account to block deposits and withdrawals. (Daniel)
11. A bank customer should be able to view the total balance across all of their accounts. (Daniel)
12. A bank customer should be able to list all of their accounts. (Bobby)
13. A bank administrator should be able to view all customers in the bank. (Bobby)
14. A bank customer should be able to set password for their account (Rosie)
15. A bank customer should be able to view the interest rate for a savings account. (Nina)
16. A bank administrator should be able to manage the interest rate for a savings account. (Nina)
---

# What commands are needed to compile and run your code from the command line?

From the project root:

Run the app with the required script:

* `./runApp.sh help`
* `./runApp.sh create-account CUST-001 123 CHECKING 100.00`
* `./runApp.sh deposit ACC-0001 50.00` (use the account id printed by `create-account`)
* `./runApp.sh withdraw ACC-0001 123 25.00 123` (use the account id printed by `create-account`)
* `./runApp.sh check-balance ACC-0001` (use the account id printed by `create-account`)
* `./runApp.sh total-balance CUST-001`
* `./runApp.sh transaction-history ACC-0001`
* `./runApp.sh transfer ACC-0001 ACC-0002 10.00 123`
* `./runApp.sh close-account ACC-0001 123`
* `./runApp.sh collect-fee admin admin123 ACC-0001 5.00`
* `./runApp.sh add-interest admin admin123 ACC-0001 3.00`
* `./runApp.sh freeze-account admin admin123 ACC-0001`
* `./runApp.sh unfreeze-account admin admin123 ACC-0001`
* `./runApp.sh clear-data` (wipes the local database and re-seeds the demo customer `CUST-001`)
* `./runApp.sh list-accounts CUST-001`
* `./runApp.sh list-customers admin admin123`

Notes:

* `runApp.sh` compiles the Java sources and runs the app without requiring Gradle to launch it.
* On the first run, the script downloads `sqlite-jdbc-3.47.2.0.jar` into `lib/`.
* Windows users should run the script from a Bash-compatible shell such as Git Bash or WSL.
* Gradle is still used for tests: `./gradlew test` on macOS/Linux or `.\gradlew.bat test` in PowerShell on Windows.
* Reliable invocation:
  * In Git Bash or WSL: `./runApp.sh help`
  * From PowerShell: `bash ./runApp.sh help`

**Persistence:** Account data is stored in a local SQLite file named `bank.db` in the working directory (created on first run). Separate `./runApp.sh` invocations share this file, so balances and transaction history survive between commands. To use a different path: add `-Dbank.db.file=/absolute/path/to/bank.db` to the Java process before launching the app.

**Seeded admin credentials:** username `admin`, password `admin123`

Implemented features:

* User story **#1**: deposit into an existing account
* User story **#2**: withdraw from an account
* User story **#3**: check an account balance
* User story **#4**: view transaction history for an account
* User story **#5**: create an additional account for an existing customer
* User story **#6**: close an existing account
* User story **#7**: transfer money from one account to another
* User story **#8**: bank administrator can collect fees from existing accounts
* User story **#9**: bank administrator can add an interest payment to an existing account
* User story **#10** bank administrator can freeze and unfreeze an account to block deposits, withdrawals, and transfers
* Command-line commands: `create-account`, `deposit`, `withdraw`, `check-balance`, `transaction-history`, `close-account`, `transfer`, `collect-fee`, `add-interest`, `freeze-account`, `unfreeze-account`, `clear-data`
* User story **#11**: customer can view the total balance across all of their accounts
* Command-line commands: `create-account`, `deposit`, `withdraw`, `check-balance`, `total-balance`, `transaction-history`, `close-account`, `transfer`, `collect-fee`, `add-interest`, `clear-data`
* User story **#12**: customer can list all of their accounts
* Command-line command: `list-accounts`
* User story **#13**: bank administrator can view all customers in the bank
* Command-line command: `list-customers`
* User story **#14**: customer accounts support password-protected operations
* Password-protected commands: `create-account`, `withdraw`, `close-account`, `transfer`
* User story **#15**: customer can view the interest rate for a savings account
* Command-line command: `view-interest-rate`
* User story **#16**: bank administrator can manage the interest rate for a savings account
* Command-line command: `set-interest-rate`
* SQLite-backed storage persists customers, accounts, transaction history, and admin credentials between CLI runs

---

# What user stories do you intend to complete next iteration? (Iteration 1)

1. A bank customer should be able to deposit into an existing account. (Nina)
2. A bank customer should be able to withdraw from an account. (Rosie)
3. A bank customer should be able to check their account balance. (Bobby)
4. A bank customer should be able to view their transaction history for an account. (Bobby)
5. A bank customer should be able to create an additional account with the bank. (Daniel)
6. A bank customer should be able to close an existing account. (Daniel)
7. A bank customer should be able to transfer money from one account to another. (Nina)
8. A bank administrator should be able to collect fees from existing accounts when necessary. (Rosie)
9. A bank administrator should be able to add an interest payment to an existing account when necessary. (Daniel)

---

# Is there anything that you implemented but doesn't currently work?

No known issues at this time for user story **#5**.

