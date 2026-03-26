package org.rbi.consentservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.consentservice.enums.ConsentDocumentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestDto {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Bank ID is required")
    private UUID bankId;

    private String consentId;

    @NotNull(message = "Requested documents are required")
    private List<ConsentDocumentType> requestedDocuments;
}
