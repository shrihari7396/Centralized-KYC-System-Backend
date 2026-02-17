package org.kyc.digilockerService.repository;

import org.kyc.digilockerService.model.IdentityVerification;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryVerificationRepository {

    private final Map<String, IdentityVerification> store = new ConcurrentHashMap<>();

    // Save or Update
    public void save(IdentityVerification verification) {
        store.put(verification.getRequestId(), verification);
    }

    // Find by Request ID
    public Optional<IdentityVerification> findByRequestId(String requestId) {
        return Optional.ofNullable(store.get(requestId));
    }

    // Find by Verification Token (scan map for simplicity in mock)
    public Optional<IdentityVerification> findByVerificationToken(String token) {
        return store.values().stream()
                .filter(v -> token.equals(v.getVerificationToken()))
                .findFirst();
    }
}
