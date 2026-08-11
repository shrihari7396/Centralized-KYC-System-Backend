package org.rbi.userservice.exception;

public class InValidOtpException extends RuntimeException {
    public InValidOtpException(String message) {
        super("Invalid OTP !!! " + message);
    }
}
