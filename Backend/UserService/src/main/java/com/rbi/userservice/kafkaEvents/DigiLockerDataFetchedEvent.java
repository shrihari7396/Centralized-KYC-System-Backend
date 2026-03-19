package com.rbi.userservice.kafkaEvents;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
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
