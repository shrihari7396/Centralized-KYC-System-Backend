package org.rbi.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.rbi.userservice.DTO.RegisterDTO;
import org.rbi.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserRegistrationController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO registerDto) {
        if (userService.registerUser(registerDto)) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully ...");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User registration failed ...");
        }
    }
}
