package edu.washu.bank.exception;

public class InvalidDepositAmountException extends RuntimeException {
    public InvalidDepositAmountException(String message) {
        super(message);
    }
}