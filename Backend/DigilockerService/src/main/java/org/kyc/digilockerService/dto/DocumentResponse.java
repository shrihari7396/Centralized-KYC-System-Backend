package org.kyc.digilockerService.dto;

import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {
    private String name;
    private String dob;
    private String gender;
    private String address;
    private String documentNumber;
}
