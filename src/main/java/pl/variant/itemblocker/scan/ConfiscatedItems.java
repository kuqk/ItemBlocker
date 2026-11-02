package pl.variant.itemblocker.scan;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public synchronized void add(String owner, List<ItemStack> taken) {
        if (taken.isEmpty()) {
            return;
        }
        load();
        items.computeIfAbsent(owner, key -> new ArrayList<>()).addAll(taken);
        save();
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
     */
    public synchronized List<ItemStack> take(String owner) {
        load();
        List<ItemStack> taken = items.remove(owner);
        if (taken == null) {
            return List.of();
        }
        save();
        return taken;
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(path)) {
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            logger.warning("Could not read " + FILE_NAME + ": " + exception.getMessage());
            return;
        }
        for (String owner : yaml.getKeys(false)) {
            List<ItemStack> stacks = new ArrayList<>();
            for (Object entry : yaml.getList(owner, List.of())) {
                if (entry instanceof ItemStack stack) {
                    stacks.add(stack);
                }
            }
            items.put(owner, stacks);
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        items.forEach(yaml::set);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, yaml.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            logger.warning("Could not save " + FILE_NAME + ": " + exception.getMessage());
        }
    }
}
