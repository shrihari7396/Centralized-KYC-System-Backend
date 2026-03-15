package com.rbi.userservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private List<String> roles;
    private java.time.LocalDateTime createdAt;
}
