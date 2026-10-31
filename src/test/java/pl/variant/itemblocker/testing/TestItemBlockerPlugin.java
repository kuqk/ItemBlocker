package pl.variant.itemblocker.testing;

import org.bukkit.inventory.EquipmentSlot;
import pl.variant.itemblocker.ItemBlockerPlugin;
import pl.variant.itemblocker.integration.PlaceholderResolver;
import pl.variant.itemblocker.integration.Regions;
import pl.variant.itemblocker.restriction.WearableSlots;

import java.util.Optional;

/**
 * The plugin as loaded in tests. MockBukkit does not know the default equipment slots of items,
 * so armor slots are derived from vanilla item names instead. WorldGuard and PlaceholderAPI are
 * replaced by fakes: {@link FakeRegions} and a resolver that fills in {@code %player_name%}.
 */
public class TestItemBlockerPlugin extends ItemBlockerPlugin {

    private final FakeRegions fakeRegions = new FakeRegions();

    public FakeRegions fakeRegions() {
        return fakeRegions;
    }

    @Override
    protected Regions loadRegions() {
        return fakeRegions;
    }

    @Override
    protected PlaceholderResolver loadPlaceholderResolver() {
        return (player, text) -> text.replace("%player_name%", player.getName());
    }

    @Override
    protected WearableSlots wearableSlots() {
        return item -> {
            if (item == null) {
                return Optional.empty();
            }

            String name = item.getType().name();
            if (name.endsWith("_HELMET") || name.endsWith("_HEAD") || name.endsWith("_SKULL") || name.equals("CARVED_PUMPKIN")) {
                return Optional.of(EquipmentSlot.HEAD);
            }
            if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) {
                return Optional.of(EquipmentSlot.CHEST);
            }
            if (name.endsWith("_LEGGINGS")) {
                return Optional.of(EquipmentSlot.LEGS);
            }
            return name.endsWith("_BOOTS") ? Optional.of(EquipmentSlot.FEET) : Optional.empty();
        };
    }
}
