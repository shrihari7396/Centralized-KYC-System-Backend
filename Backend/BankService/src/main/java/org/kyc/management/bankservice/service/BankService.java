package org.kyc.management.bankservice.service;

import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.entity.BankCustomer;

public interface BankService {
  BankCustomer getCustomerStatus(String userId);

  BankCustomer bypassKyc(BypassRequestDto bypassRequestDto);
}
