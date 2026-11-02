package pl.variant.itemblocker.log;

import org.bukkit.entity.Player;
import pl.variant.itemblocker.config.Settings;
import pl.variant.itemblocker.restriction.Denial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Records blocked actions: one line per action in {@code logs/<date>.log}, the most recent ones in
 * memory for {@code /ib log}, and optionally an alert on Discord.
 *
 * <p>Repeats of the same action, such as holding right click, are only recorded once per second.
 * Files are written on a background thread so a slow disk never holds up the server.
 */
public final class ActionLog {

    private static final int RECENT_SIZE = 500;
    private static final Duration REPEAT_WINDOW = Duration.ofSeconds(1);
    // Keys of recent actions are only kept to drop repeats, so the map is simply cleared when it grows.
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Path folder;
    private final Supplier<Settings> settings;
    private final Logger logger;
    private final Clock clock;
    private final DiscordWebhook discord;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ItemBlocker log writer");
        thread.setDaemon(true);
        return thread;
    });
    private final Deque<LogEntry> recent = new ArrayDeque<>();
    private final Map<String, Long> lastRecorded = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlerted = new ConcurrentHashMap<>();

    public ActionLog(Path folder, Supplier<Settings> settings, Logger logger, Clock clock) {
        this.folder = folder;
        this.settings = settings;
        this.logger = logger;
        this.clock = clock;
        this.discord = new DiscordWebhook(logger);
    }

    public void record(Player player, Denial denial) {
        Settings current = settings.get();
        if (!current.logEnabled() && current.discordWebhook().isEmpty()) {
            return;
        }

        LogEntry entry = new LogEntry(clock.instant(), player.getName(), denial.action(), denial.subject(),
                player.getWorld().getName(), denial.source().name());
        String key = entry.player() + '|' + entry.action() + '|' + entry.subject();
        long now = clock.millis();
        if (isRepeat(lastRecorded, key, now, REPEAT_WINDOW)) {
            return;
        }

        if (current.logEnabled()) {
            synchronized (recent) {
                recent.addFirst(entry);
                if (recent.size() > RECENT_SIZE) {
                    recent.removeLast();
                }
            }
            writer.execute(() -> write(entry));
        }
        if (!current.discordWebhook().isEmpty() && !isRepeat(lastAlerted, key, now, current.discordCooldown())) {
            discord.send(current.discordWebhook(), entry);
        }
    }

    /**
     * Returns the most recent entries, newest first.
     *
     * @param player only entries of this player, or {@code null} for everyone
     */
    public List<LogEntry> recent(String player, int limit) {
        List<LogEntry> result = new ArrayList<>();
        synchronized (recent) {
            Iterator<LogEntry> entries = recent.iterator();
            while (entries.hasNext() && result.size() < limit) {
                LogEntry entry = entries.next();
                if (player == null || entry.player().equalsIgnoreCase(player)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * Writes what is still queued and stops the writer thread.
     */
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Some blocked actions could not be written to the log in time");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void write(LogEntry entry) {
        Path file = folder.resolve(LocalDate.ofInstant(entry.time(), clock.getZone()) + ".log");
        try {
            Files.createDirectories(folder);
            Files.writeString(file, entry.toLine() + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            logger.warning("Could not write to " + file.getFileName() + ": " + exception.getMessage());
        }
    }

    private static boolean isRepeat(Map<String, Long> last, String key, long now, Duration window) {
        if (last.size() > MAX_TRACKED_KEYS) {
            last.clear();
        }
        Long previous = last.put(key, now);
        return previous != null && now - previous < window.toMillis();
    }
}
