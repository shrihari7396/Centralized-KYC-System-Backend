package org.kyc.management.kycmanagementservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DigiLockerDataFetchedEvent {
    private String mobileNumber;
    private String aadharNumber;
    private String panNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dob;
    private String address;
    private Map<String, String> additionalDetails;
}
