package org.rbi.userservice.DTO;

import org.rbi.userservice.Model.Role;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginResponseDto {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String token;
}
