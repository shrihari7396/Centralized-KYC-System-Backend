package org.kyc.digilockerService.service;

import java.util.List;

public interface JwtService {

    String extractUsername(String token);

    List<String> extractRoles(String token);

    boolean validateToken(String token);
}
