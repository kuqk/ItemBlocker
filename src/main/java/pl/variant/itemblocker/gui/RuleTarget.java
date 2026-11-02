package pl.variant.itemblocker.gui;

import org.bukkit.Material;
import pl.variant.itemblocker.rule.FilterRule;
import pl.variant.itemblocker.rule.ItemFilter;
import pl.variant.itemblocker.rule.ItemLimit;
import pl.variant.itemblocker.rule.ItemPattern;
import pl.variant.itemblocker.rule.ItemRule;
import pl.variant.itemblocker.rule.ItemTypes;
import pl.variant.itemblocker.rule.Preset;

import java.util.Optional;

/**
 * What the rule editor edits: the rule of an item type, of an item group or of a filter.
 */
sealed interface RuleTarget {

    Optional<ItemRule> rule(Preset preset);

    Preset withRule(Preset preset, ItemRule rule);

    Preset without(Preset preset);

    /**
     * The icon shown at the top of the editor.
     */
    Material icon(Preset preset);

    record ItemTarget(Material material) implements RuleTarget {

        @Override
        public Optional<ItemRule> rule(Preset preset) {
            return preset.item(material);
        }

        @Override
        public Preset withRule(Preset preset, ItemRule rule) {
            return preset.withItem(material, rule);
        }

        @Override
        public Preset without(Preset preset) {
            return preset.withoutItem(material).withLimit(material, ItemLimit.NONE);
        }

        @Override
        public Material icon(Preset preset) {
            return material;
        }
    }

    record GroupTarget(ItemPattern pattern) implements RuleTarget {

        @Override
        public Optional<ItemRule> rule(Preset preset) {
            return preset.group(pattern);
        }

        @Override
        public Preset withRule(Preset preset, ItemRule rule) {
            return preset.withGroup(pattern, rule);
        }

        @Override
        public Preset without(Preset preset) {
            return preset.withoutGroup(pattern);
        }

        /**
         * The first item type in the group, or a chest if none matches.
         */
        @Override
        public Material icon(Preset preset) {
            return ItemTypes.keys().stream()
                    .map(key -> ItemTypes.parse(key).orElseThrow())
                    .filter(pattern::matches)
                    .findFirst()
                    .orElse(Material.CHEST);
        }
    }

    record FilterTarget(String id) implements RuleTarget {

        Optional<ItemFilter> filter(Preset preset) {
            return preset.filter(id).map(FilterRule::filter);
        }

        @Override
        public Optional<ItemRule> rule(Preset preset) {
            return preset.filter(id).map(FilterRule::rule);
        }

        @Override
        public Preset withRule(Preset preset, ItemRule rule) {
            return preset.filter(id).map(current -> preset.withFilter(id, new FilterRule(current.filter(), rule))).orElse(preset);
        }

        Preset withFilter(Preset preset, ItemFilter filter) {
            return preset.filter(id).map(current -> preset.withFilter(id, new FilterRule(filter, current.rule()))).orElse(preset);
        }

        @Override
        public Preset without(Preset preset) {
            return preset.withoutFilter(id);
        }

        @Override
        public Material icon(Preset preset) {
            return filter(preset).map(ItemFilter::type).orElse(Material.NAME_TAG);
        }
    }
}
