package org.rbi.bankservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.bankservice.entity.DocumentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankKycInitiateRequest {
    @NotNull(message = "Bank ID is required")
    private String bankId;

    @NotNull(message = "Requested documents are required")
    private List<DocumentType> requestedDocuments;

    @NotNull(message = "User email is required")
    private String email;
}
