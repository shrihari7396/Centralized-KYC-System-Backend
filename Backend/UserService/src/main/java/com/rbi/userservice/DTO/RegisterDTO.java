package com.rbi.userservice.DTO;

import com.rbi.userservice.Model.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String passwordHash;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

