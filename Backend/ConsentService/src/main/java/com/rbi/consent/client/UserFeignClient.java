package com.rbi.consent.client;

import com.rbi.consent.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USERSERVICE")
public interface UserFeignClient {

    @GetMapping("/user/details")
    UserDto getUserDetails(@RequestParam("email") String email);
}
