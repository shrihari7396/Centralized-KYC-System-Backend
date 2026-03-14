package org.kyc.digilockerService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "UserService", path = "/user")
public interface UserClient {

    @PostMapping("/request-otp-phone")
    ResponseEntity<String> requestOTPByPhone(@RequestParam("phone") String phone);

    @PostMapping("/otp-verification")
    ResponseEntity<Object> otpVerification(@RequestBody Map<String, String> verificationRequest);
}
