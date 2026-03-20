package org.kyc.digilockerService.event;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DigiLockerDataFetchedEvent {
  private String email;
  private String aadharNumber;
  private String panNumber;
  private String firstName;
  private String lastName;
  private String middleName;
  private java.time.LocalDate dob;
  private String address;
  private Map<String, String> additionalDetails;
}
