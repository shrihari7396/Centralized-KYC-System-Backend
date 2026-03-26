package org.kyc.management.bankservice.exception;

public class KycNotVerifiedException extends RuntimeException {
    public KycNotVerifiedException(String message) {
        super(message);
    }
}
