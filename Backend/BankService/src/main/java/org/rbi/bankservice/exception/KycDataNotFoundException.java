package org.rbi.bankservice.exception;

public class KycDataNotFoundException extends RuntimeException {
    public KycDataNotFoundException(String message) {
        super(message);
    }
}
