package org.kyc.management.kycmanagementservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentResponse {
    private List<Object> documents;
}
