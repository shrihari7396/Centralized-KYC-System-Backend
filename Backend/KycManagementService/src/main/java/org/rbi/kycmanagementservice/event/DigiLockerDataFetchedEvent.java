package org.rbi.kycmanagementservice.event;

import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rbi.kycmanagementservice.entity.Address;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DigiLockerDataFetchedEvent {
    private String userId;
    private String mobileNumber;
    private String email;
    private String aadharNumber;
    private String panNumber;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dob;
    private Address address;
    private Map<String, String> additionalDetails;
}
