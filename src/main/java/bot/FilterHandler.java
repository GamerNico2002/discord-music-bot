package bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handles the /filter slash command, applying audio equalizer presets (bassboost, treble, pop, rock). */
public class FilterHandler {

    private static final Logger log = LoggerFactory.getLogger(FilterHandler.class);

    private final BotContext ctx;

    static final Map<String, String> FILTER_PRESETS = new LinkedHashMap<>() {{
        put("bassboost", "\uD83D\uDD0A Bassboost");
        put("treble", "\uD83C\uDFB5 Treble Boost");
        put("pop", "\uD83C\uDFA4 Pop");
        put("rock", "\uD83E\uDD18 Rock");
        put("off", "\u27A1\uFE0F Aus");
    }};

    public FilterHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    /** Handles the /filter command: applies the selected audio preset to the guild's player. */
    public void handleFilter(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());

        var presetOpt = event.getOption("preset");
        if (presetOpt == null) {
            event.reply(Lang.t(gid, "filter.off")).setEphemeral(true).queue();
            return;
        }

        String filter = presetOpt.getAsString().toLowerCase();
        manager.applyFilter(filter);
        log.info("[Filter] Preset '{}' angewendet fuer guild {}", filter, gid);

        String label = switch (filter) {
            case "bassboost" -> Lang.t(gid, "filter.bassboost");
            case "treble" -> Lang.t(gid, "filter.treble");
            case "pop" -> Lang.t(gid, "filter.pop");
            case "rock" -> Lang.t(gid, "filter.rock");
            default -> Lang.t(gid, "filter.off");
        };
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(label)
                .setColor(0x5865F2).build()).queue();
    }

    /**
     * Returns autocomplete choices for filter presets matching the user input.
     *
     * @return up to 25 matching preset choices
     */
    public List<Command.Choice> autocomplete(String input) {
        List<Command.Choice> choices = new ArrayList<>();
        for (Map.Entry<String, String> entry : FILTER_PRESETS.entrySet()) {
            if (choices.size() >= 25) break;
            if (input.isBlank() || entry.getKey().contains(input) || entry.getValue().toLowerCase().contains(input)) {
                choices.add(new Command.Choice(entry.getValue(), entry.getKey()));
            }
        }
        return choices;
    }
}
