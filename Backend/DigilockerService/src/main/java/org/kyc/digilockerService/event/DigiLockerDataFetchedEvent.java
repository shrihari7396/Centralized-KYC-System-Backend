package org.kyc.digilockerService.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigiLockerDataFetchedEvent {

    private Long userId;
    private List<String> documentIds;
    private LocalDateTime timestamp;
}
