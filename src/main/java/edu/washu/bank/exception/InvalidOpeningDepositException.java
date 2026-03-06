package edu.washu.bank.exception;

public class InvalidOpeningDepositException extends RuntimeException {
    public InvalidOpeningDepositException(String message) {
        super(message);
    }
}
