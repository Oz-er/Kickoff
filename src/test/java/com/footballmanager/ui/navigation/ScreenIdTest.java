package com.footballmanager.ui.navigation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenIdTest {
    @Test
    void definesSixCompleteNavigationDestinations() {
        assertEquals(6, ScreenId.values().length);
        assertTrue(Arrays.stream(ScreenId.values()).allMatch(screen ->
                !screen.title().isBlank()
                        && !screen.subtitle().isBlank()
                        && !screen.emptyTitle().isBlank()
                        && !screen.emptyMessage().isBlank()
        ));
    }
}
