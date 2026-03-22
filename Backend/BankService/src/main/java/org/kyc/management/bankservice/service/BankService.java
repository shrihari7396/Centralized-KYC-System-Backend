package org.kyc.management.bankservice.service;

import java.util.List;
import java.util.Map;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.LoginResponseDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.BankCustomer;

public interface BankService {
    BankCustomer getCustomerStatus(String userId, String bankId);

    BankCustomer bypassKyc(BypassRequestDto bypassRequestDto);

    Boolean sendOtp(OtpRequestDto otpRequestDto);

    Boolean verifyOtp(String otp, String key);

    void saveOtp(String otp, String key);

    void deleteOtp(String key);

    LoginResponseDto getLoginResponseDto(OtpVerificationDto otpVerificationDto);

    Boolean logout(String token);

    List<BankCustomer> getApprovedConsents();

    Map<String, Object> fetchKycData(String userId, String bankId);
}
