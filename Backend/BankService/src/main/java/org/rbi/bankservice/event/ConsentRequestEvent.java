package org.rbi.bankservice.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.bankservice.entity.DocumentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestEvent {
    private String userId;
    private String email;
    private String bankId;
    private String consentId;
    private List<DocumentType> requestedDocuments;
}
