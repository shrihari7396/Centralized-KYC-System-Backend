package com.rbi.userservice.controller;

import com.rbi.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/details")
    public ResponseEntity<?> getUserDetails(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserDetails(email));
    }

    @GetMapping("/kyc-status/{email}")
    public ResponseEntity<?> getStatusByEmail(@PathVariable String email) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserStatusByEmail(email));
    }
}
