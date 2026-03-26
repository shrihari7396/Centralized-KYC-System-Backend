package org.rbi.bankservice.client;

import org.rbi.bankservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USERSERVICE", path = "/user")
public interface UserFeignClient {

    @GetMapping("/details")
    UserDto getUserDetails(@RequestParam("email") String email);

    @GetMapping("/get-kyc-status/{email}")
    ResponseEntity<?> getStatusByEmail(@PathVariable String email);
}
