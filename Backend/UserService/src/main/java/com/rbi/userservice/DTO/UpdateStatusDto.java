package com.rbi.userservice.Model;

import com.rbi.userservice.Model.UserStatus;
import lombok.Data;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
