package org.rbi.consentservice.client;

import org.rbi.consentservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "USERSERVICE")
public interface UserFeignClient {
    @GetMapping("/user/details/{id}")
    UserDto getUserDetailsById(@PathVariable("id") String id);
}
