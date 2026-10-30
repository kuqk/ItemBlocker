package pl.variant.itemblocker.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelLimitsTest {

    @Test
    void blocksFromMinimumLevel() {
        LevelLimits limits = LevelLimits.NONE.with("sharpness", 3);

        assertFalse(limits.blocks("sharpness", 2));
        assertTrue(limits.blocks("sharpness", 3));
        assertTrue(limits.blocks("sharpness", 5));
        assertFalse(limits.blocks("smite", 5));
    }

    @Test
    void keysAreNormalized() {
        LevelLimits limits = new LevelLimits(Map.of("minecraft:Fire Aspect", 1, "custom:frost-bite", 2));

        assertEquals(List.of("custom:frost_bite", "fire_aspect"), List.copyOf(limits.minimumLevels().keySet()));
        assertTrue(limits.contains("FIRE_ASPECT"));
        assertTrue(limits.blocks("custom:frost_bite", 2));
    }

    @Test
    void withoutRemovesKey() {
        LevelLimits limits = LevelLimits.NONE.with("speed", 2).without("minecraft:speed");

        assertTrue(limits.isEmpty());
    }

    @Test
    void rejectsLevelsBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> LevelLimits.NONE.with("speed", 0));
    }
}
