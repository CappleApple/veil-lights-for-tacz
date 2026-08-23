package com.cappleapple.veiltaczlights.lighting;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashlightVisibilityStateTest {
    private static final UUID LOCAL = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID REMOTE = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void localToggleDoesNotDisableRemotePlayers() {
        FlashlightVisibilityState state = new FlashlightVisibilityState();

        assertFalse(state.toggleLocal());
        assertFalse(state.isEnabled(LOCAL, LOCAL));
        assertTrue(state.isEnabled(REMOTE, LOCAL));
    }

    @Test
    void synchronizedStateControlsTheMatchingPlayer() {
        FlashlightVisibilityState state = new FlashlightVisibilityState();

        state.accept(REMOTE, false, LOCAL);
        assertFalse(state.isEnabled(REMOTE, LOCAL));
        assertTrue(state.isEnabled(LOCAL, LOCAL));

        state.accept(LOCAL, false, LOCAL);
        assertFalse(state.isEnabled(LOCAL, LOCAL));
    }

    @Test
    void disconnectClearsRemoteStateWithoutChangingLocalToggle() {
        FlashlightVisibilityState state = new FlashlightVisibilityState();
        state.toggleLocal();
        state.accept(REMOTE, false, LOCAL);

        state.clearRemote();

        assertFalse(state.isEnabled(LOCAL, LOCAL));
        assertTrue(state.isEnabled(REMOTE, LOCAL));
    }
}
