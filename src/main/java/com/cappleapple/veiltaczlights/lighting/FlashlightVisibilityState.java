package com.cappleapple.veiltaczlights.lighting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class FlashlightVisibilityState {
    private final Map<UUID, Boolean> remoteStates = new HashMap<>();
    private boolean localEnabled = true;

    boolean isEnabled(UUID ownerId, UUID localPlayerId) {
        if (ownerId.equals(localPlayerId)) {
            return localEnabled;
        }
        return remoteStates.getOrDefault(ownerId, true);
    }

    boolean toggleLocal() {
        localEnabled = !localEnabled;
        return localEnabled;
    }

    void accept(UUID playerId, boolean enabled, UUID localPlayerId) {
        if (playerId.equals(localPlayerId)) {
            localEnabled = enabled;
        } else {
            remoteStates.put(playerId, enabled);
        }
    }

    void clearRemote() {
        remoteStates.clear();
    }
}
