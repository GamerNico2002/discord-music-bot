package bot;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles queue management, now-playing display, and related slash commands / button interactions.
 */
public class QueueHandler {

    private static final Logger log = LoggerFactory.getLogger(QueueHandler.class);
    private static final int QUEUE_PAGE_SIZE = 10;

    private final BotContext ctx;

    public QueueHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    public void handleQueue(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);
        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        AudioTrack current = manager.player.getPlayingTrack();

        if (tracks.isEmpty() && current == null) {
            event.reply(Lang.t(gid, "queue.empty")).queue();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(tracks.size() / (double) QUEUE_PAGE_SIZE));
        event.replyEmbeds(buildQueueEmbed(gid, tracks, current, manager, 0, totalPages).build())
                .addComponents(queueButtons(gid, 0, totalPages))
                .queue();
    }

    public EmbedBuilder buildQueueEmbed(long gid, List<AudioTrack> tracks, AudioTrack current,
                                        GuildMusicManager manager, int page, int totalPages) {
        StringBuilder sb = new StringBuilder();

        if (current != null && page == 0) {
            sb.append(Lang.t(gid, "now.playing.now")).append("\n")
              .append("`").append(ctx.formatTime(current.getPosition()))
              .append(" / ").append(ctx.formatTime(current.getDuration())).append("` ")
              .append(current.getInfo().title).append("\n\n");
        }

        if (!tracks.isEmpty()) {
            sb.append(Lang.t(gid, "queue.list")).append("\n");
            int start = page * QUEUE_PAGE_SIZE;
            int end = Math.min(start + QUEUE_PAGE_SIZE, tracks.size());
            for (int i = start; i < end; i++) {
                AudioTrack track = tracks.get(i);
                sb.append("`").append(String.format("%2d", i + 1)).append(".` ")
                  .append(track.getInfo().title)
                  .append(" `[").append(ctx.formatTime(track.getDuration())).append("]`\n");
            }
        }

        long totalMs = tracks.stream().mapToLong(AudioTrack::getDuration).sum();
        if (current != null) totalMs += current.getDuration() - current.getPosition();

        String repeatLabel = switch (manager.scheduler.getRepeatMode()) {
            case OFF -> "";
            case TRACK -> Lang.t(gid, "repeat.song.full");
            case QUEUE -> Lang.t(gid, "repeat.queue.full");
        };

        return new EmbedBuilder()
                .setTitle(Lang.t(gid, "queue.title"))
                .setDescription(sb.toString())
                .setFooter(Lang.t(gid, "queue.footer", tracks.size(), ctx.formatTimeLong(totalMs), repeatLabel))
                .setColor(0x5865F2);
    }

    private ActionRow queueButtons(long gid, int currentPage, int totalPages) {
        return ActionRow.of(
                Button.secondary("queue_prev_" + currentPage, Emoji.fromUnicode("\u25C0\uFE0F"))
                        .withDisabled(currentPage == 0),
                Button.secondary("queue_page",
                        Lang.t(gid, "page") + " " + (currentPage + 1) + "/" + totalPages)
                        .withDisabled(true),
                Button.secondary("queue_next_" + currentPage, Emoji.fromUnicode("\u25B6\uFE0F"))
                        .withDisabled(currentPage >= totalPages - 1)
        );
    }

    public void handleNowPlaying(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long guildId = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);
        AudioTrack track = manager.player.getPlayingTrack();
        boolean nonstop = ctx.nonstopGuilds.contains(guildId);

        if (track == null && !nonstop) {
            event.reply(Lang.t(guildId, "nothing.playing")).queue();
            return;
        }

        cancelNpUpdate(guildId);

        EmbedBuilder embed = track != null
                ? buildNowPlayingEmbed(guildId, track, manager)
                : buildLoadingEmbed(guildId);
        event.replyEmbeds(embed.build())
                .addComponents(npButtons(manager))
                .queue(hook -> startNpAutoUpdate(guildId, manager, hook));
    }

    public void startNpAutoUpdate(long guildId, GuildMusicManager manager, InteractionHook hook) {
        ScheduledFuture<?> oldTask = ctx.npUpdateTasks.remove(guildId);
        if (oldTask != null) oldTask.cancel(false);
        ctx.npHooks.put(guildId, hook);
        ScheduledFuture<?> task = ctx.npScheduler.scheduleAtFixedRate(() -> {
            try {
                if (ctx.npHooks.get(guildId) != hook) return;
                AudioTrack current = manager.player.getPlayingTrack();
                boolean nonstop = ctx.nonstopGuilds.contains(guildId);
                if (current == null && !nonstop) return;
                EmbedBuilder embed = current != null
                        ? buildNowPlayingEmbed(guildId, current, manager)
                        : buildLoadingEmbed(guildId);
                hook.editOriginalEmbeds(embed.build())
                        .setComponents(npButtons(manager)).queue(null, err -> {});
            } catch (Exception ignored) {}
        }, 3, 3, TimeUnit.SECONDS);
        ctx.npUpdateTasks.put(guildId, task);
    }

    public void cancelNpUpdate(long guildId) {
        ScheduledFuture<?> task = ctx.npUpdateTasks.remove(guildId);
        if (task != null) task.cancel(false);
        InteractionHook oldHook = ctx.npHooks.remove(guildId);
        if (oldHook != null) {
            try {
                oldHook.editOriginalComponents().queue(null, err -> {});
            } catch (Exception ignored) {}
        }
    }

    private EmbedBuilder buildNowPlayingEmbed(long gid, AudioTrack track, GuildMusicManager manager) {
        long pos = track.getPosition();
        long dur = track.getDuration();
        long remaining = dur - pos;
        String position = ctx.formatTime(pos) + " / " + ctx.formatTime(dur);
        String progressBar = ctx.buildProgressBar(pos, dur);
        String repeatLabel = switch (manager.scheduler.getRepeatMode()) {
            case OFF -> "";
            case TRACK -> Lang.t(gid, "repeat.song.short");
            case QUEUE -> Lang.t(gid, "repeat.queue.short");
        };

        String status = manager.player.isPaused()
                ? Lang.t(gid, "pause")
                : Lang.t(gid, "now.playing.status.vol", manager.player.getVolume());

        return new EmbedBuilder()
                .setTitle(Lang.t(gid, "now.playing.title"))
                .setDescription("**" + track.getInfo().title + "**\n" +
                        track.getInfo().author + "\n\n" +
                        progressBar + "\n" +
                        "`" + position + "`  \u23F3 `-" + ctx.formatTime(remaining) + "`" + repeatLabel + "\n\n" +
                        status)
                .setColor(0x5865F2)
                .setThumbnail("https://img.youtube.com/vi/" + ctx.extractVideoId(track.getInfo().uri) + "/0.jpg");
    }

    private EmbedBuilder buildLoadingEmbed(long gid) {
        return new EmbedBuilder()
                .setTitle(Lang.t(gid, "now.playing.title"))
                .setDescription(Lang.t(gid, "nonstop.loading"))
                .setColor(0x5865F2);
    }

    ActionRow npButtons(GuildMusicManager manager) {
        return ActionRow.of(
                Button.secondary("np_restart", Emoji.fromUnicode("\u23EE")),
                Button.primary("np_pause", Emoji.fromUnicode(manager.player.isPaused() ? "\u25B6\uFE0F" : "\u23F8\uFE0F")),
                Button.secondary("np_skip", Emoji.fromUnicode("\u23ED")),
                Button.danger("np_stop", Emoji.fromUnicode("\u23F9\uFE0F"))
        );
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        String id = event.getComponentId();

        if (id.startsWith("queue_")) {
            handleQueueButton(event);
            return;
        }

        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        long guildId = event.getGuild().getIdLong();

        switch (id) {
            case "np_restart" -> {
                AudioTrack track = manager.player.getPlayingTrack();
                if (track != null) {
                    track.setPosition(0);
                    event.editMessageEmbeds(buildNowPlayingEmbed(guildId, track, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                }
            }
            case "np_pause" -> {
                manager.player.setPaused(!manager.player.isPaused());
                AudioTrack track = manager.player.getPlayingTrack();
                if (track != null) {
                    event.editMessageEmbeds(buildNowPlayingEmbed(guildId, track, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else {
                    event.reply(Lang.t(guildId, "nothing.playing")).setEphemeral(true).queue();
                }
            }
            case "np_skip" -> {
                manager.scheduler.skip();
                AudioTrack next = manager.player.getPlayingTrack();
                if (next != null) {
                    event.editMessageEmbeds(buildNowPlayingEmbed(guildId, next, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else if (ctx.nonstopGuilds.contains(guildId)) {
                    event.editMessageEmbeds(buildLoadingEmbed(guildId).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else {
                    cancelNpUpdate(guildId);
                    event.editMessage(Lang.t(guildId, "skipped.empty.short"))
                            .setComponents().setEmbeds().queue();
                }
            }
            case "np_stop" -> {
                cancelNpUpdate(guildId);
                ctx.nonstopGuilds.remove(guildId);
                manager.scheduler.stop();
                event.editMessage(Lang.t(guildId, "stop.short")).setComponents().setEmbeds().queue();
            }
        }
    }

    private void handleQueueButton(ButtonInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        AudioTrack current = manager.player.getPlayingTrack();
        int totalPages = Math.max(1, (int) Math.ceil(tracks.size() / (double) QUEUE_PAGE_SIZE));

        String id = event.getComponentId();
        int currentPage = 0;
        if (id.startsWith("queue_prev_")) {
            currentPage = Math.max(0, Integer.parseInt(id.substring(11)) - 1);
        } else if (id.startsWith("queue_next_")) {
            currentPage = Math.min(totalPages - 1, Integer.parseInt(id.substring(11)) + 1);
        }

        int page = currentPage;
        event.editMessageEmbeds(buildQueueEmbed(gid, tracks, current, manager, page, totalPages).build())
                .setComponents(queueButtons(gid, page, totalPages))
                .queue();
    }

    public void handleMove(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);

        var fromOpt = event.getOption("von");
        var toOpt = event.getOption("nach");
        if (fromOpt == null || toOpt == null) {
            event.reply(Lang.t(gid, "pos.invalid", manager.scheduler.getQueue().size()))
                    .setEphemeral(true).queue();
            return;
        }

        int from = fromOpt.getAsInt();
        int to = toOpt.getAsInt();
        int size = manager.scheduler.getQueue().size();
        if (from < 1 || from > size || to < 1 || to > size) {
            event.reply(Lang.t(gid, "pos.invalid", size)).setEphemeral(true).queue();
            return;
        }

        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        String title = tracks.get(from - 1).getInfo().title;
        if (manager.scheduler.moveInQueue(from - 1, to - 1)) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(gid, "moved", title, from, to))
                    .setColor(0x5865F2).build()).queue();
        } else {
            event.reply(Lang.t(gid, "move.failed")).setEphemeral(true).queue();
        }
    }

    public void handleRemove(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);

        var posOpt = event.getOption("position");
        if (posOpt == null) {
            event.reply(Lang.t(gid, "pos.invalid", manager.scheduler.getQueue().size()))
                    .setEphemeral(true).queue();
            return;
        }

        int pos = posOpt.getAsInt();
        int size = manager.scheduler.getQueue().size();
        if (pos < 1 || pos > size) {
            event.reply(Lang.t(gid, "pos.invalid", size)).setEphemeral(true).queue();
            return;
        }

        AudioTrack removed = manager.scheduler.removeFromQueue(pos - 1);
        if (removed != null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(gid, "removed", removed.getInfo().title, pos))
                    .setColor(0xED4245).build()).queue();
        } else {
            event.reply(Lang.t(gid, "remove.failed")).setEphemeral(true).queue();
        }
    }

    public void handleClear(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);
        int size = manager.scheduler.getQueue().size();
        if (size == 0) {
            event.reply(Lang.t(gid, "queue.already.empty")).setEphemeral(true).queue();
            return;
        }
        manager.scheduler.clearQueue();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "cleared", size))
                .setColor(0xED4245).build()).queue();
    }

    public void handleShuffle(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);
        if (manager.scheduler.getQueue().isEmpty()) {
            event.reply(Lang.t(gid, "queue.empty")).setEphemeral(true).queue();
            return;
        }
        manager.scheduler.shuffle();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "shuffled", manager.scheduler.getQueue().size()))
                .setColor(0x5865F2).build()).queue();
    }

    public void handleRepeat(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);

        var modeOpt = event.getOption("mode");
        if (modeOpt == null) {
            event.reply(Lang.t(gid, "repeat.invalid")).setEphemeral(true).queue();
            return;
        }

        String mode = modeOpt.getAsString().toUpperCase();
        try {
            TrackScheduler.RepeatMode repeatMode = TrackScheduler.RepeatMode.valueOf(mode);
            manager.scheduler.setRepeatMode(repeatMode);
            String label = switch (repeatMode) {
                case OFF -> Lang.t(gid, "repeat.off");
                case TRACK -> Lang.t(gid, "repeat.track.set");
                case QUEUE -> Lang.t(gid, "repeat.queue.set");
            };
            event.replyEmbeds(new EmbedBuilder().setDescription(label).setColor(0x5865F2).build()).queue();
        } catch (IllegalArgumentException e) {
            event.reply(Lang.t(gid, "repeat.invalid")).setEphemeral(true).queue();
        }
    }

    public void handleSkipTo(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);

        var posOpt = event.getOption("position");
        if (posOpt == null) {
            event.reply(Lang.t(gid, "pos.invalid", manager.scheduler.getQueue().size()))
                    .setEphemeral(true).queue();
            return;
        }

        int pos = posOpt.getAsInt();
        int size = manager.scheduler.getQueue().size();
        if (pos < 1 || pos > size) {
            event.reply(Lang.t(gid, "pos.invalid", size)).setEphemeral(true).queue();
            return;
        }

        AudioTrack track = manager.scheduler.skipTo(pos - 1);
        if (track != null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(gid, "skipto.success", pos, track.getInfo().title))
                    .setColor(0x5865F2).build()).queue();
        } else {
            event.reply(Lang.t(gid, "skipto.failed")).setEphemeral(true).queue();
        }
    }

    public void handleSave(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(guild);
        AudioTrack track = manager.player.getPlayingTrack();
        if (track == null) {
            event.reply(Lang.t(gid, "nothing.playing")).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "save.title"))
                .setDescription("**" + track.getInfo().title + "**\n" +
                        track.getInfo().author + "\n\n" +
                        Lang.t(gid, "save.open") + ": " + track.getInfo().uri)
                .setThumbnail("https://img.youtube.com/vi/" + ctx.extractVideoId(track.getInfo().uri) + "/0.jpg")
                .setColor(0x57F287);

        event.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageEmbeds(embed.build()).queue(
                        success -> event.reply(Lang.t(gid, "save.dm.sent")).setEphemeral(true).queue(),
                        fail -> event.reply(Lang.t(gid, "save.dm.failed")).setEphemeral(true).queue()
                ),
                fail -> event.reply(Lang.t(gid, "save.dm.failed.short")).setEphemeral(true).queue()
        );
    }

    public void handleVolume(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        long gid = guild.getIdLong();

        var volOpt = event.getOption("vol");
        if (volOpt == null) {
            event.reply(Lang.t(gid, "volume.range")).setEphemeral(true).queue();
            return;
        }

        int vol = volOpt.getAsInt();
        if (vol < 0 || vol > 100) {
            event.reply(Lang.t(gid, "volume.range")).setEphemeral(true).queue();
            return;
        }

        GuildMusicManager manager = ctx.getGuildMusic(guild);
        manager.player.setVolume(vol);
        String bar = ctx.buildVolumeBar(vol);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "volume.set", vol, bar))
                .setColor(0x5865F2).build()).queue();
    }
}
