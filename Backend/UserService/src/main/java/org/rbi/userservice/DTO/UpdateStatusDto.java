package org.rbi.userservice.DTO;

import lombok.Data;
import org.rbi.userservice.Model.UserStatus;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
