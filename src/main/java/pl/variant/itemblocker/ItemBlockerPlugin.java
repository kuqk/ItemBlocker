package pl.variant.itemblocker;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.variant.itemblocker.command.ItemBlockerCommand;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.integration.Integrations;
import pl.variant.itemblocker.integration.PlaceholderResolver;
import pl.variant.itemblocker.integration.Regions;
import pl.variant.itemblocker.listener.ArmorListener;
import pl.variant.itemblocker.listener.ContainerListener;
import pl.variant.itemblocker.listener.CraftingListener;
import pl.variant.itemblocker.listener.EnchantmentListener;
import pl.variant.itemblocker.listener.ItemListener;
import pl.variant.itemblocker.listener.PlacementListener;
import pl.variant.itemblocker.listener.PotionEffectListener;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.platform.PlatformExtension;
import pl.variant.itemblocker.restriction.ArmorEnforcer;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.restriction.Notifier;
import pl.variant.itemblocker.restriction.RestrictionService;
import pl.variant.itemblocker.restriction.WearableSlots;
import pl.variant.itemblocker.storage.RuleFile;
import pl.variant.itemblocker.storage.RuleRepository;
import pl.variant.itemblocker.storage.legacy.LegacyMigration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class ItemBlockerPlugin extends JavaPlugin {

    private volatile Settings settings = Settings.DEFAULTS;
    private Messages messages;
    private RuleRepository rules;
    private Regions regions = Regions.NONE;
    private Enforcement enforcement;

    @Override
    public void onEnable() {
        Path dataFolder = getDataFolder().toPath();
        migrateLegacyFiles(dataFolder);

        saveDefaultConfig();
        regions = loadRegions();
        messages = new Messages(dataFolder.resolve("lang"), this::getResource, getLogger(), loadPlaceholderResolver());
        rules = new RuleRepository(new RuleFile(dataFolder.resolve(RuleFile.FILE_NAME), getLogger()));
        try {
            reload();
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not load " + RuleFile.FILE_NAME
                    + ", no rules are active until it is fixed and reloaded", exception);
        }

        Notifier notifier = new Notifier(messages, () -> settings.messageCooldown());
        enforcement = new Enforcement(new RestrictionService(rules::rules, () -> settings, regions), notifier);
        ArmorEnforcer armorEnforcer = new ArmorEnforcer(enforcement);
        WearableSlots wearableSlots = wearableSlots();

        List<Listener> listeners = List.of(
                notifier,
                new ItemListener(enforcement, wearableSlots),
                new PlacementListener(enforcement),
                new CraftingListener(enforcement),
                new ArmorListener(enforcement, armorEnforcer, wearableSlots),
                new ContainerListener(enforcement),
                new EnchantmentListener(enforcement),
                new PotionEffectListener(enforcement)
        );
        listeners.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
        PlatformExtension.forServer(getClassLoader()).ifPresent(extension -> extension.enable(this, armorEnforcer));

        PluginCommand command = Objects.requireNonNull(getCommand("itemblocker"), "itemblocker command missing from plugin.yml");
        ItemBlockerCommand executor = new ItemBlockerCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /**
     * Reloads config.yml, the messages and rules.yml.
     *
     * @throws IOException if rules.yml cannot be loaded; the previous rules stay active
     */
    public void reload() throws IOException {
        reloadConfig();
        settings = Settings.from(getConfig());
        messages.load(settings.language());
        rules.reload();
        if (rules.rules().usesRegions() && !regions.isAvailable()) {
            getLogger().warning("Some presets are limited to regions, but WorldGuard is not installed."
                    + " Those presets do not apply anywhere until it is.");
        }
    }

    /**
     * Returns how armor slots of items are determined. Tests replace this because mock servers do
     * not know the default equipment slots of items.
     */
    protected WearableSlots wearableSlots() {
        return WearableSlots.fromItemTypes();
    }

    /**
     * Returns the region lookup. Tests replace this because mock servers have no WorldGuard.
     */
    protected Regions loadRegions() {
        return Integrations.load(this, "WorldGuard", Regions.class).orElse(Regions.NONE);
    }

    protected PlaceholderResolver loadPlaceholderResolver() {
        return Integrations.load(this, "PlaceholderAPI", PlaceholderResolver.class).orElse(PlaceholderResolver.NONE);
    }

    public Messages messages() {
        return messages;
    }

    public RuleRepository rules() {
        return rules;
    }

    public Regions regions() {
        return regions;
    }

    public Enforcement enforcement() {
        return enforcement;
    }

    private void migrateLegacyFiles(Path dataFolder) {
        LegacyMigration migration = new LegacyMigration(dataFolder, getLogger());
        if (!migration.isNeeded()) {
            return;
        }

        try (InputStream defaultConfig = getResource("config.yml")) {
            if (defaultConfig == null) {
                throw new IOException("The bundled config.yml is missing");
            }
            migration.migrate(new String(defaultConfig.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not migrate the ItemBlocker 1.x files, they were left unchanged", exception);
        }
    }
}
