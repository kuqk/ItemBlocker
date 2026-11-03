package pl.variant.itemblocker.command;

import org.bukkit.command.CommandSender;
import pl.variant.itemblocker.message.Message;
import pl.variant.itemblocker.message.Placeholder;
import pl.variant.itemblocker.rule.Preset;
import pl.variant.itemblocker.storage.PresetTemplates;

import java.util.List;
import java.util.Set;

/**
 * {@code /ib template} lists the bundled preset templates, {@code /ib template <name> [as:<preset>]}
 * creates a preset from one.
 */
final class TemplateCommand implements Subcommand {

    @Override
    public String name() {
        return "template";
    }

    @Override
    public boolean isAvailableTo(CommandSender sender) {
        return sender.hasPermission(Permissions.PRESET);
    }

    @Override
    public List<Message> help() {
        return List.of(Message.HELP_TEMPLATE);
    }

    @Override
    public void execute(CommandContext context) {
        context.requirePermission(Permissions.PRESET);
        PresetTemplates templates = context.plugin().templates();
        String name = context.arg(0);
        if (name == null) {
            context.send(Message.TEMPLATE_HEADER);
            templates.all().forEach(template -> context.send(Message.TEMPLATE_ENTRY,
                    Placeholder.of("template", template.name()),
                    Placeholder.of("description", template.description())));
            return;
        }

        Preset template = templates.find(name).orElseThrow(() -> new CommandException(Message.TEMPLATE_UNKNOWN,
                Placeholder.of("input", name), Placeholder.of("templates", String.join(", ", templates.names()))));
        Options options = Options.parse(context.argsFrom(1), Set.of(Options.AS));
        String presetName = options.get(Options.AS).orElse(template.name());
        Placeholder preset = Placeholder.of("preset", presetName);
        if (!Preset.isValidName(presetName)) {
            throw new CommandException(Message.PRESET_INVALID_NAME, preset);
        }
        if (context.rules().preset(presetName).isPresent()) {
            throw new CommandException(Message.PRESET_EXISTS, preset);
        }

        context.updateRules(rules -> rules.with(template.withName(presetName)));
        context.send(Message.TEMPLATE_CREATED, Placeholder.of("preset", Preset.normalizeName(presetName)),
                Placeholder.of("template", template.name()));
    }

    @Override
    public List<String> complete(CommandContext context) {
        if (context.args().size() == 1) {
            return Completions.matching(context.arg(0), context.plugin().templates().names());
        }
        return Completions.options(context, List.of(Options.AS));
    }
}
