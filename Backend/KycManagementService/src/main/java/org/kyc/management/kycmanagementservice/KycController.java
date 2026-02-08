package org.kyc.management.kycmanagementservice;

import org.kyc.management.kycmanagementservice.dto.DocumentResponse;
import org.kyc.management.kycmanagementservice.entity.MockUser;
import org.kyc.management.kycmanagementservice.service.DocumentProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kyc")
public class KycController {
    @Autowired
    private DocumentProviderService service;

    @GetMapping("/user/{adhar}")
    public ResponseEntity<MockUser> getUserDetails(@PathVariable String adhar) {
        MockUser user =   service.getProfile(adhar);
        if(user == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
    @GetMapping("/documents/{userId}")
    public ResponseEntity<?> getdocuments(String userId){
        DocumentResponse docs=  service.getDocument(userId);
        if(docs == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(docs);
    }


}
