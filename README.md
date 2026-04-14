# project26

## Team Members

* Daniel Yan
* Bobby Yu
* Rosie Tang
* Nina Chen

---

# Features planned to implement in iteration 3:
1. Reconstruct the user interface into a text-based user interface.


# Getting Started

From the project root:

Run the app with the required script:

* `./runApp.sh`

## Notes:

* `runApp.sh` compiles the Java sources and runs the app without requiring Gradle to launch it.
* On the first run, the script downloads `sqlite-jdbc-3.47.2.0.jar` into `lib/`.
* Windows users should run the script from a Bash-compatible shell such as Git Bash or WSL.
* Gradle is still used for tests: `./gradlew test` on macOS/Linux or `.\gradlew.bat test` in PowerShell on Windows.

## Initial customer and admin account
### Customer
ID: CUST-001
Name: Demo User
Password: password

### Admin
Username: admin
Password: admin123


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
* Command-line commands: `create-account`, `deposit`, `withdraw`, `check-balance`, `transaction-history`, `close-account`, `transfer`, `collect-fee`, `add-interest`, `clear-data`
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

# Features planned to implement this iteration (iteration 2)

10. A bank administrator should be able to freeze and unfreeze an account to block deposits and withdrawals. (Daniel)
11. A bank customer should be able to view the total balance across all of their accounts. (Daniel)
12. A bank customer should be able to list all of their accounts. (Bobby)
13. A bank administrator should be able to view all customers in the bank. (Bobby)
---

# Is there anything that you implemented but doesn't currently work?

No known issues at this time for user story **#5**.

