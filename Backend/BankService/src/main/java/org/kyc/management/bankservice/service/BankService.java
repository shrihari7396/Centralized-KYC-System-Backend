package org.kyc.management.bankservice.service;

import java.util.List;
import java.util.Map;
import org.kyc.management.bankservice.dto.BypassRequestDto;
import org.kyc.management.bankservice.dto.DashboardStatsResponse;
import org.kyc.management.bankservice.dto.LoginResponseDto;
import org.kyc.management.bankservice.dto.OtpRequestDto;
import org.kyc.management.bankservice.dto.OtpVerificationDto;
import org.kyc.management.bankservice.entity.Bank;
import org.kyc.management.bankservice.entity.BankCustomer;
import org.kyc.management.bankservice.entity.BankCustomerKycData;
import org.kyc.management.bankservice.entity.Consent;

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

    Bank getBankProfile(String email);

    List<BankCustomer> getCustomersByBank(String bankId);

    DashboardStatsResponse getDashboardStats(String bankId);

    List<Consent> getPendingConsents(String email);

    List<Consent> getAllConsentsByBank(String bankId);

    List<Bank> getAllBanks();

    List<BankCustomerKycData> getKycHistory(String userId, String bankId);
}
