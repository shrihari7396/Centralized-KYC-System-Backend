package com.rbi.userservice.Model;

import lombok.Data;

@Data
public class UpdateStatusDto {
    private String email;
    private UserStatus status;
}
