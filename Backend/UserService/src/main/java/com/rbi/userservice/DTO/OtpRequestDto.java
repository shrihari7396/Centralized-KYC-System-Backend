package com.rbi.userservice.DTO;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpRequestDto {
    private String email;
}
