package com.rbi.consent.event;

import com.rbi.consent.enums.ConsentDocumentType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequestEvent {
    private String userId;
    private String email;
    private String bankId;
    private String consentId;
    private List<ConsentDocumentType> requestedDocuments;
}
