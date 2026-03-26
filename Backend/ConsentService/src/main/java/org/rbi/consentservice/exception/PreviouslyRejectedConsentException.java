package org.rbi.consentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PreviouslyRejectedConsentException extends RuntimeException {
    public PreviouslyRejectedConsentException(String message) {
        super(message);
    }
}
