package org.kyc.management.bankservice.client;

import org.kyc.management.bankservice.entity.MockUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "KycManagementService")
public interface DocumentFetch {
    @GetMapping("/user/{adhar}")
    MockUser getUserDetails(@PathVariable String adhar);

    @GetMapping("/documents/{userId}")
    DocumentResponse getdocuments(String userId);

}
