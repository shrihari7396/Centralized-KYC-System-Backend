package org.kyc.management.bankservice.exception;

public class AlreadyVerifiedException extends RuntimeException {
    public AlreadyVerifiedException(String message) {
        super(message);
    }
}
