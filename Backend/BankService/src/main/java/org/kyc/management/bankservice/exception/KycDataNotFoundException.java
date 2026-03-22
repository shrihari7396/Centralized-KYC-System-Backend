package org.kyc.management.bankservice.exception;

public class KycDataNotFoundException extends RuntimeException {
    public KycDataNotFoundException(String message) {
        super(message);
    }
}
