package org.kyc.notificationService.service;

import java.util.Set;

public interface SessionRegistry {

    void registerSession(String userId, String sessionId);

    void unregisterSession(String userId, String sessionId);

    String getInstanceForUser(String userId);

    boolean isUserOnline(String userId);

    Set<String> getLocalSessionsForUser(String userId);
}
