package com.cappleapple.veiltaczlights.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class PlayerFlashlightStateStore {
    private final Map<UUID, Boolean> states = new HashMap<>();

    boolean isEnabled(UUID playerId) {
        return states.getOrDefault(playerId, true);
    }

    void set(UUID playerId, boolean enabled) {
        states.put(playerId, enabled);
    }

    void reset(UUID playerId) {
        states.remove(playerId);
    }

    void clear() {
        states.clear();
    }
}
