package org.kyc.digilockerService.service;

import io.jsonwebtoken.Claims;

public interface JwtService {

    String extractEmail(String token);

    boolean isValid(String token);

}
