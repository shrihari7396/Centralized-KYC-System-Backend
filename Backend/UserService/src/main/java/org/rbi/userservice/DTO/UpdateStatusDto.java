package org.rbi.userservice.DTO;

import org.rbi.userservice.Model.UserStatus;
import lombok.Data;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
