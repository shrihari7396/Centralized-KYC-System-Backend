package org.kyc.management.bankservice.service;

import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.LoginResponseDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.BankCustomer;

public interface BankService {
    BankCustomer getCustomerStatus(String userId);

    BankCustomer bypassKyc(BypassRequestDto bypassRequestDto);

    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean verifyOtp(String otp, String key);

    void saveOtp(String otp, String key);

    void deleteOtp(String key);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Boolean logout(String token);

    java.util.List<BankCustomer> getApprovedConsents();

    java.util.Map<String, Object> fetchKycData(String userId);
}
