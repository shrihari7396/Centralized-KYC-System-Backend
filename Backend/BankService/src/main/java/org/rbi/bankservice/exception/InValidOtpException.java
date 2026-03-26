package org.rbi.bankservice.exception;

public class InValidOtpException extends RuntimeException {
    public InValidOtpException(String message) {
        super(message);
    }
}
