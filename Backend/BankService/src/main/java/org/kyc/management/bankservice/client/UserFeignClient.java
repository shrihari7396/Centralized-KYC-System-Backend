package org.kyc.management.bankservice.client;

import org.kyc.management.bankservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USER-SERVICE", path = "/user")
public interface UserFeignClient {

    @GetMapping("/details")
    UserDto getUserDetails(@RequestParam("email") String email);
}
