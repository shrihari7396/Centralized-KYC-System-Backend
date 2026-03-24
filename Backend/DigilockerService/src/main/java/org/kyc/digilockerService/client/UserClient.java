package org.kyc.digilockerService.client;

import org.kyc.digilockerService.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USERSERVICE", path = "/user")
public interface UserClient {

    @GetMapping("/details")
    UserDto getUserDetails(@RequestParam("email") String email);
}
