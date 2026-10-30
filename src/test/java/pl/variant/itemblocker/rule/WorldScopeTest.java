package pl.variant.itemblocker.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldScopeTest {

    @Test
    void emptyScopeIncludesEveryWorld() {
        assertTrue(WorldScope.ALL.isAll());
        assertTrue(WorldScope.ALL.includes("world"));
        assertTrue(WorldScope.ALL.includes(null));
    }

    @Test
    void namedScopeMatchesWorldsCaseInsensitively() {
        WorldScope scope = WorldScope.of(List.of("World", " world_nether "));

        assertEquals(Set.of("world", "world_nether"), scope.worlds());
        assertTrue(scope.includes("WORLD"));
        assertFalse(scope.includes("world_the_end"));
        assertFalse(scope.includes(null));
    }

    @Test
    void allKeywordMeansEveryWorld() {
        assertSame(WorldScope.ALL, WorldScope.of(List.of("world", "ALL")));
        assertSame(WorldScope.ALL, WorldScope.of(List.of(" ", "")));
    }

    @Test
    void unionWithEveryWorldIsEveryWorld() {
        WorldScope scope = WorldScope.of(List.of("world"));

        assertTrue(scope.union(WorldScope.ALL).isAll());
        assertEquals(
                Set.of("world", "spawn"),
                scope.union(WorldScope.of(List.of("spawn"))).worlds()
        );
    }
}
