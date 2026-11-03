package pl.variant.itemblocker.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.variant.itemblocker.platform.PlayerScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lets a menu ask for text: the player's next chat message is taken as the answer instead of being
 * sent to the chat. Typing {@value #CANCEL} or clicking the cancel button, which runs
 * {@value #CANCEL_COMMAND}, cancels.
 */
public final class ChatPrompt implements Listener {

    static final String CANCEL = "cancel";
    static final String CANCEL_COMMAND = "/itemblocker cancel";

    private record Question(Consumer<String> answer, Runnable cancelled) {
    }

    private final PlayerScheduler scheduler;
    private final Map<UUID, Question> questions = new ConcurrentHashMap<>();

    public ChatPrompt(PlayerScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Waits for the player's next chat message. Both callbacks run on the player's thread.
     */
    void ask(Player player, Consumer<String> answer, Runnable cancelled) {
        questions.put(player.getUniqueId(), new Question(answer, cancelled));
    }

    /**
     * Answers a question as if the player had typed the text, which is also how tests answer.
     *
     * @return whether the player was asked something
     */
    boolean answer(Player player, String text) {
        Question question = questions.remove(player.getUniqueId());
        if (question == null) {
            return false;
        }

        String trimmed = text.trim();
        scheduler.runNextTick(player, () -> {
            if (trimmed.equalsIgnoreCase(CANCEL)) {
                question.cancelled().run();
            } else {
                question.answer().accept(trimmed);
            }
        });
        return true;
    }

    // The legacy chat event is the one every supported server fires; Paper's own chat event does not
    // exist on Spigot.
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (answer(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    /**
     * The cancel button runs {@value #CANCEL_COMMAND}. It is handled here and never reaches the
     * command, also when the question was already answered.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().trim().equalsIgnoreCase(CANCEL_COMMAND)) {
            event.setCancelled(true);
            answer(event.getPlayer(), CANCEL);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        questions.remove(event.getPlayer().getUniqueId());
    }
}
