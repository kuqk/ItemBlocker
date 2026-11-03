package pl.variant.itemblocker;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import pl.variant.itemblocker.api.ItemBlockerApi;
import pl.variant.itemblocker.command.ItemBlockerCommand;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.gui.ChatPrompt;
import pl.variant.itemblocker.gui.Gui;
import pl.variant.itemblocker.gui.MenuListener;
import pl.variant.itemblocker.integration.Integrations;
import pl.variant.itemblocker.integration.PlaceholderResolver;
import pl.variant.itemblocker.integration.Regions;
import pl.variant.itemblocker.listener.AcquisitionListener;
import pl.variant.itemblocker.listener.ArmorListener;
import pl.variant.itemblocker.listener.CombatListener;
import pl.variant.itemblocker.listener.ContainerListener;
import pl.variant.itemblocker.listener.CraftingListener;
import pl.variant.itemblocker.listener.EnchantmentFixListener;
import pl.variant.itemblocker.listener.EnchantmentListener;
import pl.variant.itemblocker.listener.ItemListener;
import pl.variant.itemblocker.listener.LimitListener;
import pl.variant.itemblocker.listener.PlacementListener;
import pl.variant.itemblocker.listener.PotionEffectListener;
import pl.variant.itemblocker.log.ActionLog;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.platform.PlatformExtension;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.platform.RegionScheduler;
import pl.variant.itemblocker.restriction.ArmorEnforcer;
import pl.variant.itemblocker.restriction.EnchantmentFixer;
import pl.variant.itemblocker.restriction.Enforcement;
import pl.variant.itemblocker.restriction.LimitEnforcer;
import pl.variant.itemblocker.restriction.Notifier;
import pl.variant.itemblocker.restriction.RestrictionService;
import pl.variant.itemblocker.restriction.TemporaryRules;
import pl.variant.itemblocker.restriction.WearableSlots;
import pl.variant.itemblocker.scan.ConfiscatedItems;
import pl.variant.itemblocker.scan.Scanner;
import pl.variant.itemblocker.storage.RuleFile;
import pl.variant.itemblocker.storage.RuleRepository;
import pl.variant.itemblocker.storage.legacy.LegacyMigration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public class ItemBlockerPlugin extends JavaPlugin {

    private volatile Settings settings = Settings.DEFAULTS;
    private Messages messages;
    private RuleRepository rules;
    private Regions regions = Regions.NONE;
    private Enforcement enforcement;
    private Gui gui;
    private ActionLog actionLog;
    private TemporaryRules temporaryRules;
    private Scanner scanner;
    private ConfiscatedItems confiscated;

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

        Optional<PlatformExtension> platform = PlatformExtension.forServer(getClassLoader());
        PlayerScheduler scheduler = platform.map(extension -> extension.scheduler(this))
                .orElseGet(() -> PlayerScheduler.bukkit(this));

        Notifier notifier = new Notifier(messages, () -> settings.messageCooldown());
        actionLog = new ActionLog(dataFolder.resolve("logs"), () -> settings, getLogger(), Clock.systemDefaultZone());
        temporaryRules = new TemporaryRules();
        RestrictionService restrictions = new RestrictionService(() -> temporaryRules.merge(rules.rules()), () -> settings, regions,
                temporaryRules, getServer().getPluginManager()::callEvent);
        enforcement = new Enforcement(restrictions, notifier, actionLog);
        ArmorEnforcer armorEnforcer = new ArmorEnforcer(enforcement);
        WearableSlots wearableSlots = wearableSlots();

        ChatPrompt chatPrompt = new ChatPrompt(scheduler);
        gui = new Gui(rules, messages, scheduler, chatPrompt, regions);
        RegionScheduler regionScheduler = platform.map(extension -> extension.regionScheduler(this))
                .orElseGet(() -> RegionScheduler.bukkit(this));
        confiscated = new ConfiscatedItems(dataFolder.resolve(ConfiscatedItems.FILE_NAME), getLogger());
        scanner = new Scanner(enforcement.restrictions(), scheduler, regionScheduler, confiscated);

        List<Listener> listeners = List.of(
                notifier,
                new ItemListener(enforcement, wearableSlots),
                new PlacementListener(enforcement),
                new CraftingListener(enforcement),
                new ArmorListener(enforcement, armorEnforcer, wearableSlots),
                new ContainerListener(enforcement),
                new EnchantmentListener(enforcement),
                new PotionEffectListener(enforcement),
                new CombatListener(enforcement),
                new AcquisitionListener(enforcement),
                new LimitListener(new LimitEnforcer(enforcement), scheduler),
                new EnchantmentFixListener(new EnchantmentFixer(enforcement, confiscated), scheduler, rules::rules),
                new MenuListener(),
                chatPrompt,
                temporaryRules
        );
        listeners.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
        registerIfSupported("org.bukkit.event.block.CrafterCraftEvent", "pl.variant.itemblocker.modern.CrafterListener");
        platform.ifPresent(extension -> extension.enable(this, armorEnforcer));

        PluginCommand command = Objects.requireNonNull(getCommand("itemblocker"), "itemblocker command missing from plugin.yml");
        ItemBlockerCommand executor = new ItemBlockerCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getServer().getServicesManager().register(ItemBlockerApi.class, new ItemBlockerService(restrictions, temporaryRules),
                this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (actionLog != null) {
            actionLog.close();
        }
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

    public Gui gui() {
        return gui;
    }

    public ActionLog actionLog() {
        return actionLog;
    }

    public Scanner scanner() {
        return scanner;
    }

    public ConfiscatedItems confiscated() {
        return confiscated;
    }

    /**
     * Registers a listener for an event that only newer servers have, if this server has it.
     */
    private void registerIfSupported(String eventClass, String listenerClass) {
        try {
            Class.forName(eventClass);
        } catch (ClassNotFoundException exception) {
            return;
        }

        try {
            Listener listener = (Listener) Class.forName(listenerClass).getConstructor(Enforcement.class).newInstance(enforcement);
            getServer().getPluginManager().registerEvents(listener, this);
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().log(Level.WARNING, "Could not enable " + listenerClass, exception);
        }
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
