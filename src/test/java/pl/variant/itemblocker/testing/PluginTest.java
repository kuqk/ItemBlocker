package pl.variant.itemblocker.testing;

import org.bukkit.Material;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import pl.variant.itemblocker.ItemBlockerPlugin;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Base class for tests that need the plugin running on a mock server, with one player who has no
 * bypass permissions.
 */
public abstract class PluginTest {

    protected ServerMock server;
    protected TestItemBlockerPlugin plugin;
    protected PlayerMock player;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.loadWith(TestItemBlockerPlugin.class, description());
        player = server.addPlayer();
        player.setOp(false);
    }

    @AfterEach
    void stopServer() {
        MockBukkit.unmock();
    }

    /**
     * The real plugin.yml with the test plugin class as the main class, so commands and
     * permissions are registered exactly as on a server.
     */
    private static PluginDescriptionFile description() {
        try (InputStream stream = PluginTest.class.getResourceAsStream("/plugin.yml")) {
            String yaml = new String(Objects.requireNonNull(stream, "plugin.yml").readAllBytes(), StandardCharsets.UTF_8)
                    .replace("main: " + ItemBlockerPlugin.class.getName(), "main: " + TestItemBlockerPlugin.class.getName());
            return new PluginDescriptionFile(new StringReader(yaml));
        } catch (IOException | InvalidDescriptionException exception) {
            throw new IllegalStateException("Could not read plugin.yml", exception);
        }
    }

    protected void block(Material material, Action... actions) {
        updateDefault(preset -> preset.withItem(material, new ItemRule(EnumSet.copyOf(List.of(actions)), WorldScope.ALL)));
    }

    protected void blockEnchantment(String key, int minimumLevel) {
        updateDefault(preset -> preset.withEnchantments(preset.enchantments().with(key, minimumLevel)));
    }

    protected void blockEffect(String key, int minimumLevel) {
        updateDefault(preset -> preset.withEffects(preset.effects().with(key, minimumLevel)));
    }

    protected void updateDefault(UnaryOperator<Preset> change) {
        updateRules(rules -> rules.with(change.apply(rules.defaultPreset())));
    }

    protected void updateRules(UnaryOperator<RuleBook> change) {
        try {
            plugin.rules().update(change);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Returns and clears the messages the player received, without colors.
     */
    protected List<String> messages() {
        List<String> messages = new ArrayList<>();
        for (String message = player.nextMessage(); message != null; message = player.nextMessage()) {
            messages.add(Text.plain(message));
        }
        return messages;
    }
}
