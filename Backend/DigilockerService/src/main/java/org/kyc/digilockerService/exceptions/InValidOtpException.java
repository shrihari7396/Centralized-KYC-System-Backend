package org.kyc.digilockerService.exceptions;

public class InValidOtpException extends RuntimeException {
  public InValidOtpException(String msg) {
    super(msg);
  }
}
