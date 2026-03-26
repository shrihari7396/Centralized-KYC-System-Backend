package org.rbi.bankservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rbi.bankservice.dto.RiskUpdateRequest;
import org.rbi.bankservice.entity.BankCustomer;
import org.rbi.bankservice.entity.KycStatus;
import org.rbi.bankservice.service.BankService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bank/kyc")
@RequiredArgsConstructor
@Slf4j
public class BankCustomerController {

    private final BankService bankSvc;

    @PostMapping("/fetch/{userId}/{bankId}")
    public ResponseEntity<Map<String, Object>> fetchCustomerData(
            @PathVariable("userId") String userId, @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.fetchKycData(userId, bankId));
    }

    @GetMapping("/customers/{bankId}")
    public ResponseEntity<List<BankCustomer>> getCustomersByBank(
            @PathVariable("bankId") String bankId) {
        return ResponseEntity.ok(bankSvc.getCustomersByBank(bankId));
    }

    @PostMapping("/risk/reclassify")
    public ResponseEntity<BankCustomer> updateRisk(@RequestBody @Valid RiskUpdateRequest request) {
        return ResponseEntity.ok(bankSvc.updateRiskLevel(request));
    }

    @GetMapping("/customers/search")
    public ResponseEntity<Page<BankCustomer>> searchCustomers(
            @RequestParam String bankId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) KycStatus status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                bankSvc.searchCustomers(
                        bankId, query, status, riskLevel, PageRequest.of(page, size)));
    }
}
