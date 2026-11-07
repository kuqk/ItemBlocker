package pl.variant.itemblocker.scan;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Items taken away by a scan, kept in {@code confiscated.yml} by owner until an admin takes them.
 * An owner is a player name or the location of a container.
 */
public final class ConfiscatedItems {

    public static final String FILE_NAME = "confiscated.yml";

    private final Path path;
    private final Logger logger;
    private final Map<String, List<ItemStack>> items = new LinkedHashMap<>();
    private boolean loaded;

    public ConfiscatedItems(Path path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    /** Adds items only after their new state has been saved; failures leave memory unchanged. */
    public synchronized void add(String owner, List<ItemStack> taken) {
        if (taken.isEmpty()) {
            return;
        }
        load();
        Map<String, List<ItemStack>> updated = new LinkedHashMap<>(items);
        List<ItemStack> stacks = new ArrayList<>(items.getOrDefault(owner, List.of()));
        taken.forEach(item -> stacks.add(item.clone()));
        updated.put(owner, stacks);
        save(updated);
        items.put(owner, stacks);
    }

    /**
     * Returns how many stacks each owner has confiscated.
     */
    public synchronized Map<String, Integer> owners() {
        load();
        Map<String, Integer> counts = new LinkedHashMap<>();
        items.forEach((owner, stacks) -> counts.put(owner, stacks.size()));
        return counts;
    }

    /**
     * Removes and returns every item confiscated from the owner.
     * Nothing is issued if the removal cannot be saved.
     */
    public synchronized List<ItemStack> take(String owner) {
        load();
        List<ItemStack> taken = items.get(owner);
        if (taken == null) {
            return List.of();
        }
        Map<String, List<ItemStack>> updated = new LinkedHashMap<>(items);
        updated.remove(owner);
        save(updated);
        items.remove(owner);
        return taken.stream().map(ItemStack::clone).toList();
    }

    private void load() {
        if (loaded) {
            return;
        }
        YamlConfiguration yaml = configuration();
        try {
            yaml.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
            Map<String, List<ItemStack>> restored = new LinkedHashMap<>();
            readOwners(yaml, "", restored);
            items.putAll(restored);
            loaded = true;
        } catch (NoSuchFileException exception) {
            loaded = true;
        } catch (InvalidConfigurationException exception) {
            throw failure("read", new IOException(exception.getMessage(), exception));
        } catch (IOException exception) {
            throw failure("read", exception);
        }
    }

    private static void readOwners(ConfigurationSection section, String prefix,
                                   Map<String, List<ItemStack>> restored) throws IOException {
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            String owner = prefix + entry.getKey();
            if (entry.getValue() instanceof ConfigurationSection nested) {
                // Older versions interpreted dots in world names as YAML paths. Recover those entries too.
                readOwners(nested, owner + ".", restored);
            } else if (entry.getValue() instanceof List<?> entries) {
                List<ItemStack> stacks = new ArrayList<>();
                for (Object item : entries) {
                    if (!(item instanceof ItemStack stack)) {
                        throw new IOException("Invalid confiscated item for " + owner);
                    }
                    stacks.add(stack.clone());
                }
                if (restored.putIfAbsent(owner, stacks) != null) {
                    throw new IOException("Duplicate confiscation owner " + owner);
                }
            } else {
                throw new IOException("Invalid confiscation entry for " + owner);
            }
        }
    }

    private void save(Map<String, List<ItemStack>> updated) {
        YamlConfiguration yaml = configuration();
        updated.forEach(yaml::set);
        Path temporary = null;
        try {
            Path target = path.toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "confiscated-", ".tmp");
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw failure("save", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    logger.warning("Could not remove temporary confiscation file: " + exception.getMessage());
                }
            }
        }
    }

    private UncheckedIOException failure(String operation, IOException exception) {
        String message = "Could not " + operation + " " + FILE_NAME + ": " + exception.getMessage();
        logger.warning(message);
        return new UncheckedIOException(message, exception);
    }

    private static YamlConfiguration configuration() {
        YamlConfiguration yaml = new YamlConfiguration();
        // Owner names are literal keys, including dots in a world's name.
        yaml.options().pathSeparator('\0');
        return yaml;
    }
}
