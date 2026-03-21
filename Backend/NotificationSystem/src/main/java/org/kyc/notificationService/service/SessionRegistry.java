package org.kyc.notificationService.service;

import java.util.Set;

public interface SessionRegistry {

    void registerSession(String targetId, String sessionId);

    void unregisterSession(String targetId, String sessionId);

    String getInstanceForUser(String targetId);

    boolean isUserOnline(String targetId);

    Set<String> getLocalSessionsForUser(String targetId);
}
