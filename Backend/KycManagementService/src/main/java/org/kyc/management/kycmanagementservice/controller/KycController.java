package org.kyc.management.kycmanagementservice.controller;

import java.util.Map;
import org.kyc.management.kycmanagementservice.dto.DigilockerDataRequest;
import org.kyc.management.kycmanagementservice.entity.KycEntity;
import org.kyc.management.kycmanagementservice.service.KycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

  @Autowired private KycService kycService;

  /** Endpoint for Digilocker to POST user data. */
  @PostMapping("/digilocker")
  public ResponseEntity<KycEntity> storeDigilockerData(@RequestBody DigilockerDataRequest request) {
    KycEntity savedEntity = kycService.storeDigilockerData(request);
    return ResponseEntity.ok(savedEntity);
  }

  /** Endpoint for Banks to fetch user documents dynamically relying on JWT fields. */
  @GetMapping("/bank/documents")
  public ResponseEntity<?> getDocumentsForBank(@RequestHeader("Authorization") String token) {
    try {
      Map<String, Object> data = kycService.getVerifiedDataForBank(token);
      return ResponseEntity.ok(data);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (RuntimeException e) {
      return ResponseEntity.status(403).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("An error occurred processing the request");
    }
  }
}
