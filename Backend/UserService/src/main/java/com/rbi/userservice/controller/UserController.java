package com.rbi.userservice.controller;

import com.rbi.userservice.DTO.RegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class UserController {

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDTO registerDto) {
        return null;
    }
}
