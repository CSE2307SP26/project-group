package edu.washu.bank.exception;

public class InvalidAccountClosureException extends RuntimeException {
    public InvalidAccountClosureException(String message) {
        super(message);
    }
}
