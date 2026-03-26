package org.rbi.bankservice.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "consents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {
    @Id private String id;

    private String bankId;
    private String userId;
    private KycStatus status;
    private List<DocumentType> requestedDocuments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
