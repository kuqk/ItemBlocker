package pl.variant.itemblocker.storage;

import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ready-made presets bundled with the plugin, such as survival, lifesteal and pvp, which become a
 * normal preset once created.
 */
public final class PresetTemplates {

    public static final String RESOURCE = "templates.yml";

    private final List<Preset> templates;

    private PresetTemplates(List<Preset> templates) {
        this.templates = templates;
    }

    /**
     * Reads the bundled templates. Items, enchantments or effects the server does not know are left
     * out without warnings, since templates cover every supported version.
     */
    public static PresetTemplates load(Function<String, InputStream> resources, Logger logger) {
        try (InputStream stream = resources.apply(RESOURCE)) {
            if (stream == null) {
                return new PresetTemplates(List.of());
            }
            Logger quiet = Logger.getAnonymousLogger();
            quiet.setLevel(Level.OFF);
            quiet.setUseParentHandlers(false);
            RuleBook book = new RuleFile(Path.of(RESOURCE), quiet).parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            return new PresetTemplates(book.presets().stream()
                    .filter(preset -> !preset.name().equals(RuleBook.DEFAULT_PRESET))
                    .toList());
        } catch (IOException exception) {
            logger.warning("Could not read the bundled " + RESOURCE + ": " + exception.getMessage());
            return new PresetTemplates(List.of());
        }
    }

    public List<Preset> all() {
        return templates;
    }

    public Optional<Preset> find(String name) {
        String normalized = Preset.normalizeName(name);
        return templates.stream().filter(template -> template.name().equals(normalized)).findFirst();
    }

    public List<String> names() {
        return templates.stream().map(Preset::name).toList();
    }
}
