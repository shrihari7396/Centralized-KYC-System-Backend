package org.kyc.management.kycmanagementservice.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentResponse {
  private List<Object> documents;
}
