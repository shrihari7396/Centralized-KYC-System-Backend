package org.kyc.digilockerService.dto;

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
public class DigiLockerFetchResponseDto {

    private Long userId;
    private List<DigiLockerDocumentDto> documents;
    private LocalDateTime fetchedAt;
}
