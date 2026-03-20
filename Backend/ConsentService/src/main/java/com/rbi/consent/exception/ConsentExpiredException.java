package com.rbi.consent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class ConsentExpiredException extends RuntimeException {
  public ConsentExpiredException(String message) {
    super(message);
  }
}
