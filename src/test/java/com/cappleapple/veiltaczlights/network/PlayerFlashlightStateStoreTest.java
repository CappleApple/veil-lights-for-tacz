package com.cappleapple.veiltaczlights.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerFlashlightStateStoreTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void defaultsToEnabledAndResetsAtSessionBoundaries() {
        PlayerFlashlightStateStore states = new PlayerFlashlightStateStore();

        assertTrue(states.isEnabled(PLAYER));
        states.set(PLAYER, false);
        assertFalse(states.isEnabled(PLAYER));

        states.reset(PLAYER);
        assertTrue(states.isEnabled(PLAYER));
    }

    @Test
    void serverStopClearsAllPlayerStates() {
        PlayerFlashlightStateStore states = new PlayerFlashlightStateStore();
        states.set(PLAYER, false);

        states.clear();

        assertTrue(states.isEnabled(PLAYER));
    }
}
