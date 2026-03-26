package org.rbi.userservice.DTO;

import org.rbi.userservice.Model.UserStatus;
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
    private UserStatus status;
}
