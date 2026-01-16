package com.rbi.userservice.DTO;

import com.rbi.userservice.Model.UserStatus;
import lombok.*;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
@Builder
@Data
@RequiredArgsConstructor
public class RegisterDTO {
    private String email;

    String firstName;

    String lastName;


    private String mobileNumber;


    private String passwordHash;


    private UserStatus status;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
