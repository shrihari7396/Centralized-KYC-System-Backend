package org.rbi.userservice.DTO;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtpRequestDto {
    private String firstName;
    private String lastName;
    private String email;
}
