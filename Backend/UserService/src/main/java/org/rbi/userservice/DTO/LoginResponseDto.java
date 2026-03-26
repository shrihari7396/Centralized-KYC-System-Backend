package org.rbi.userservice.DTO;

import lombok.Builder;
import lombok.Data;
import org.rbi.userservice.Model.Role;

@Builder
@Data
public class LoginResponseDto {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String token;
}
