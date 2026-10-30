package pl.variant.itemblocker.restriction;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.message.Placeholder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Tells players why something was blocked, at most once per cooldown so that repeated attempts
 * such as holding right click do not flood the chat.
 */
public final class Notifier implements Listener {

    private final Messages messages;
    private final Supplier<Duration> cooldown;
    private final Map<UUID, Long> lastSent = new ConcurrentHashMap<>();

    public Notifier(Messages messages, Supplier<Duration> cooldown) {
        this.messages = messages;
        this.cooldown = cooldown;
    }

    public void notify(Player player, Denial denial) {
        long now = System.nanoTime();
        long cooldownNanos = cooldown.get().toNanos();
        Long previous = lastSent.get(player.getUniqueId());
        if (previous != null && now - previous < cooldownNanos) {
            return;
        }
        lastSent.put(player.getUniqueId(), now);

        String reason = denial.source().reason();
        List<Placeholder> placeholders = new ArrayList<>(denial.placeholders());
        placeholders.add(Placeholder.of("reason", reason.isBlank()
                ? ""
                : messages.render(Message.RESTRICTION_REASON, Placeholder.of("reason", reason))));
        messages.send(player, denial.message(), placeholders.toArray(Placeholder[]::new));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSent.remove(event.getPlayer().getUniqueId());
    }
}
