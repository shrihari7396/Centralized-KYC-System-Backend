package org.kyc.management.bankservice.exception;

public class ConsentNotFoundException extends RuntimeException {
    public ConsentNotFoundException(String message) {
        super(message);
    }
}
