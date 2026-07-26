package bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Handles /help, /info, /ping, /invite, /dcleave and /language slash commands. */
public class InfoHandler {

    private static final Logger log = LoggerFactory.getLogger(InfoHandler.class);

    private final BotContext ctx;

    public InfoHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    /** Handles the /help command: displays a categorized list of all available commands. */
    public void handleHelp(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "help.title"))
                .setColor(0x5865F2)
                .addField(Lang.t(gid, "help.music.title"), Lang.t(gid, "help.music.body"), false)
                .addField(Lang.t(gid, "help.queue.title"), Lang.t(gid, "help.queue.body"), false)
                .addField(Lang.t(gid, "help.voice.title"), Lang.t(gid, "help.voice.body"), false)
                .addField(Lang.t(gid, "help.other.title"), Lang.t(gid, "help.other.body"), false)
                .setFooter(Lang.t(gid, "help.footer"));
        event.replyEmbeds(embed.build()).queue();
    }

    /** Handles the /info command: shows bot uptime, server/member counts, CPU, and memory stats. */
    public void handleInfo(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();

        Duration uptime = Duration.between(MusicBot.START_TIME, Instant.now());
        String uptimeStr = formatUptime(uptime);

        long activePlayers = ctx.musicManagers.entrySet().stream()
                .filter(e -> {
                    Guild g = event.getJDA().getGuildById(e.getKey());
                    if (g == null) return false;
                    if (!g.getAudioManager().isConnected()) return false;
                    return e.getValue().player.getPlayingTrack() != null;
                })
                .count();

        var guilds = event.getJDA().getGuilds();
        int totalMembers = guilds.stream()
                .mapToInt(Guild::getMemberCount)
                .sum();

        String selfName = event.getJDA().getSelfUser().getName();
        String selfAvatar = event.getJDA().getSelfUser().getEffectiveAvatarUrl();

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        int ramPercent = (int) (usedMb * 100 / maxMb);

        double cpuLoad = -1;
        long sysTotalMb = 0, sysUsedMb = 0;
        int sysRamPercent = 0;
        try {
            var osBean = (com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            cpuLoad = osBean.getProcessCpuLoad() * 100;
            sysTotalMb = osBean.getTotalMemorySize() / (1024 * 1024);
            long sysFreeMb = osBean.getFreeMemorySize() / (1024 * 1024);
            sysUsedMb = sysTotalMb - sysFreeMb;
            sysRamPercent = sysTotalMb > 0 ? (int) (sysUsedMb * 100 / sysTotalMb) : 0;
        } catch (Exception ignored) {}

        String cpuStr = cpuLoad >= 0 ? String.format("%.1f%%", cpuLoad) : "N/A";
        String sysRamStr = sysTotalMb > 0
                ? sysUsedMb + " / " + sysTotalMb + " MB (" + sysRamPercent + "%)"
                : "N/A";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83E\uDD16 " + selfName)
                .setThumbnail(selfAvatar)
                .setColor(0x5865F2)
                .addField(Lang.t(gid, "info.owner"), ctx.ownerDisplay, true)
                .addField(Lang.t(gid, "info.servers"), String.valueOf(guilds.size()), true)
                .addField(Lang.t(gid, "info.members"), String.valueOf(totalMembers), true)
                .addField(Lang.t(gid, "info.uptime"), uptimeStr, true)
                .addField(Lang.t(gid, "info.active.players"), activePlayers + " / " + guilds.size(), true)
                .addField("\u200B", "\u200B", true)
                .addField(Lang.t(gid, "uptime.cpu"), cpuStr, true)
                .addField(Lang.t(gid, "uptime.threads"), String.valueOf(Thread.activeCount()), true)
                .addField("\u200B", "\u200B", true)
                .addField(Lang.t(gid, "uptime.bot.ram"), usedMb + " / " + maxMb + " MB (" + ramPercent + "%)", true)
                .addField(Lang.t(gid, "uptime.sys.ram"), sysRamStr, true)
                .addField(Lang.t(gid, "info.support"), ctx.supportContact, false)
                .addField(Lang.t(gid, "info.discord"), Lang.t(gid, "info.discord.value"), false)
                .setFooter(Lang.t(gid, "info.footer"));
        event.replyEmbeds(embed.build()).queue();
    }

    /** Handles the /ping command: shows the current gateway WebSocket latency. */
    public void handlePing(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        long gatewayPing = event.getJDA().getGatewayPing();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "ping.title"))
                .setColor(0x5865F2)
                .addField(Lang.t(gid, "ping.gateway"), gatewayPing + "ms", true);
        event.replyEmbeds(embed.build()).queue();
    }

    /** Handles the /invite command: sends a bot invite link with required permissions. */
    public void handleInvite(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        String botId = event.getJDA().getSelfUser().getId();
        String url = "https://discord.com/oauth2/authorize?client_id=" + botId
                + "&permissions=3145728&scope=bot%20applications.commands";
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "invite.title"))
                .setDescription(Lang.t(gid, "invite.desc", url))
                .setColor(0x57F287);
        event.replyEmbeds(embed.build()).queue();
    }

    /** Handles the /dcleave command: forces the bot to leave a specified server (owner-only). */
    public void handleDcLeave(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        if (ctx.ownerId.isEmpty() || !event.getUser().getId().equals(ctx.ownerId)) {
            log.warn("[dcleave] Nicht-Owner {} hat /dcleave versucht", event.getUser().getId());
            event.reply(Lang.t(gid, "dcleave.not.owner")).setEphemeral(true).queue();
            return;
        }

        var serverOpt = event.getOption("server");
        if (serverOpt == null) {
            event.reply(Lang.t(gid, "dcleave.not.found", "?")).setEphemeral(true).queue();
            return;
        }

        String guildId = serverOpt.getAsString();
        Guild target = event.getJDA().getGuildById(guildId);
        if (target == null) {
            event.reply(Lang.t(gid, "dcleave.not.found", guildId)).setEphemeral(true).queue();
            return;
        }

        String name = target.getName();
        log.info("[dcleave] Owner verlaesst Server: {} ({})", name, guildId);
        event.reply(Lang.t(gid, "dcleave.leaving", name, guildId)).setEphemeral(true).queue();
        target.leave().queue(
                s -> log.info("[dcleave] Server verlassen: {} ({})", name, guildId),
                err -> log.error("[dcleave] Fehler beim Verlassen von {}: {}", name, err.getMessage())
        );
    }

    /** Handles the /language command: changes the bot's response language for the current guild. */
    public void handleLanguage(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        var codeOpt = event.getOption("code");

        if (codeOpt == null) {
            String current = Lang.SUPPORTED.get(Lang.getLang(guildId));
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle(Lang.t(guildId, "lang.title"))
                    .setDescription(Lang.t(guildId, "lang.current", current))
                    .setColor(0x5865F2).build()).queue();
            return;
        }

        String code = codeOpt.getAsString();
        if (!Lang.SUPPORTED.containsKey(code)) {
            event.reply("Unsupported language code: " + code).setEphemeral(true).queue();
            return;
        }

        Lang.setLang(guildId, code);
        String label = Lang.SUPPORTED.get(code);
        log.info("[Lang] Sprache fuer guild {} geaendert auf {}", guildId, code);
        event.replyEmbeds(new EmbedBuilder()
                .setTitle(Lang.t(guildId, "lang.title"))
                .setDescription(Lang.t(guildId, "lang.changed", label))
                .setColor(0x57F287).build()).queue();
    }

    /**
     * Formats a {@link Duration} as a human-readable uptime string like {@code "2d 5h 30m 12s"}.
     */
    public String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        sb.append(minutes).append("m ").append(seconds).append("s");
        return sb.toString();
    }

    /**
     * Returns autocomplete choices for the /dcleave server parameter.
     *
     * @return up to 25 matching guild choices
     */
    public List<Command.Choice> autocompleteDcLeave(CommandAutoCompleteInteractionEvent event, String input) {
        List<Command.Choice> choices = new ArrayList<>();
        for (Guild guild : event.getJDA().getGuilds()) {
            if (choices.size() >= 25) break;
            String name = guild.getName();
            if (input.isBlank() || name.toLowerCase().contains(input) || guild.getId().contains(input)) {
                choices.add(new Command.Choice(name.length() > 100 ? name.substring(0, 97) + "..." : name, guild.getId()));
            }
        }
        return choices;
    }
}
