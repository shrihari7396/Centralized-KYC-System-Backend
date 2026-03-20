package com.rbi.consent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ConsentRejectedException extends RuntimeException {
  public ConsentRejectedException(String message) {
    super(message);
  }
}
