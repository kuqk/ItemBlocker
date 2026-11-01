package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.variant.itemblocker.message.Formats;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Messages;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.platform.PlayerScheduler;
import pl.variant.itemblocker.rule.Action;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.rule.RuleBook;
import pl.variant.itemblocker.rule.WorldScope;
import pl.variant.itemblocker.storage.RuleRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * What menus share: the rules they edit, the messages they show and moving between menus.
 */
public final class Gui {

    private final RuleRepository rules;
    private final Messages messages;
    private final PlayerScheduler scheduler;

    public Gui(RuleRepository rules, Messages messages, PlayerScheduler scheduler) {
        this.rules = rules;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    /**
     * Opens the preset overview.
     */
    public void openPresets(Player player) {
        new PresetsMenu(this).open(player);
    }

    /**
     * Opens a menu on the next tick. Opening an inventory from inside a click is not allowed.
     */
    void open(Player player, Menu menu) {
        scheduler.runNextTick(player, () -> menu.open(player));
    }

    void close(Player player) {
        scheduler.runNextTick(player, player::closeInventory);
    }

    RuleBook rules() {
        return rules.rules();
    }

    Optional<Preset> preset(String name) {
        return rules.rules().preset(name);
    }

    Messages messages() {
        return messages;
    }

    /**
     * Applies and saves a change to a preset. If that fails, the player is told why and the menu
     * closes.
     *
     * @return whether the change was saved
     */
    boolean updatePreset(Player player, String name, UnaryOperator<Preset> change) {
        try {
            rules.update(book -> book.preset(name).map(preset -> book.with(change.apply(preset))).orElse(book));
            return true;
        } catch (IOException exception) {
            messages.send(player, Message.ERROR_SAVE_FAILED, Placeholder.of("error", exception.getMessage()));
            close(player);
            return false;
        }
    }

    String text(Message message, Placeholder... placeholders) {
        return messages.render(message, placeholders);
    }

    List<String> lines(Message message, Placeholder... placeholders) {
        return Arrays.asList(text(message, placeholders).split("\n"));
    }

    /**
     * Lore lines describing the rule and limits of an item in a preset.
     */
    List<String> summary(Message message, Preset preset, Material material) {
        Optional<ItemRule> rule = preset.item(material);
        ItemLimit limit = preset.limit(material);
        Set<Action> actions = rule.map(ItemRule::actions).orElseGet(() -> EnumSet.noneOf(Action.class));
        return lines(message,
                Placeholder.of("actions", Formats.actions(messages, actions)),
                Placeholder.of("worlds", Formats.worlds(messages, rule.map(ItemRule::scope).orElse(WorldScope.ALL))),
                Placeholder.of("stack", Formats.limit(messages, limit.stack())),
                Placeholder.of("inventory", Formats.limit(messages, limit.inventory())));
    }

    static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    ItemStack icon(Material material, Message name, Placeholder... placeholders) {
        return icon(material, text(name, placeholders), List.of());
    }

    static ItemStack filler() {
        return icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }
}
