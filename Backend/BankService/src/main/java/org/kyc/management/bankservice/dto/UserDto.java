package org.kyc.management.bankservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private List<String> roles;
    private String status;

    // Explicit constructor for testing
    public UserDto(String userId, String firstName, String lastName, String email, String phone, List<String> roles, String status) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.roles = roles;
        this.status = status;
    }
}
