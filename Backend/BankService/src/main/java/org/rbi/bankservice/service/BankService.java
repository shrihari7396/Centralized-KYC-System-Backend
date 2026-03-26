package org.rbi.bankservice.service;

import java.util.List;
import java.util.Map;
import org.rbi.bankservice.dto.*;
import org.rbi.bankservice.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Bank getBankById(String bankId);

    List<BankCustomerKycData> getKycHistory(String userId, String bankId);

    Bank registerBank(BankRegisterRequest req);

    List<TimelineEvent> getUnifiedTimeline(String userId, String bankId);

    BankCustomer updateRiskLevel(RiskUpdateRequest request);

    Page<BankCustomer> searchCustomers(
            String bankId, String query, KycStatus status, String riskLevel, Pageable pageable);

    BankCustomerKycData findByConsentId(String consentId);

    Map<DocumentType, String> getDetailReport(Long kycRequestId);
}
