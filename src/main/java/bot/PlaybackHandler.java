package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class PlaybackHandler {

    private static final Logger log = LoggerFactory.getLogger(PlaybackHandler.class);
    private static final long AUTO_NONSTOP_DELAY_MS = 2 * 60 * 1000L;

    private final BotContext ctx;

    public PlaybackHandler(BotContext ctx) {
        this.ctx = ctx;
    }

    public void handlePlay(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }

        OptionMapping queryOpt = event.getOption("query");
        if (queryOpt == null) {
            event.reply(Lang.t(gid, "play.no.query")).setEphemeral(true).queue();
            return;
        }
        String query = queryOpt.getAsString();

        event.deferReply().queue();

        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = ctx.getGuildMusic(guild);

        boolean wasNonstop = ctx.nonstopGuilds.remove(guildId);
        NonstopHandler.cancelAutoNonstop(ctx, guildId);
        if (wasNonstop) {
            musicManager.scheduler.clearQueue();
            musicManager.player.stopTrack();
            log.info("[Nonstop] Gestoppt durch /play in guild {}", guildId);
        }

        if (ctx.spotify.isSpotifyUrl(query)) {
            handleSpotifyPlay(event, guild, channel, musicManager, query);
            return;
        }

        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            query = "ytsearch:" + query;
        }

        loadAndPlay(event, guild, channel, musicManager, query, false);
    }

    private void handleSpotifyPlay(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel,
                                   GuildMusicManager musicManager, String url) {
        long gid = guild.getIdLong();
        if (!ctx.spotify.isConfigured()) {
            event.getHook().sendMessage(Lang.t(gid, "spotify.not.configured")).queue();
            return;
        }
        ctx.spotify.resolveAsync(url).thenAcceptAsync(searches -> {
            if (searches.isEmpty()) {
                event.getHook().sendMessage(Lang.t(gid, "spotify.no.songs")).queue();
                return;
            }
            if (searches.size() == 1) {
                loadSpotifySingle(event, guild, channel, musicManager, searches.get(0), gid);
            } else {
                loadSpotifyMultiple(event, guild, channel, musicManager, searches, gid);
            }
        }, ctx.npScheduler).exceptionally(e -> {
            event.getHook().sendMessage(Lang.t(gid, "spotify.error", e.getCause().getMessage())).queue();
            return null;
        });
    }

    private void loadSpotifySingle(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel,
                                   GuildMusicManager musicManager, String search, long gid) {
        ctx.playerManager.loadItemOrdered(musicManager, search, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                ctx.connectAndPlay(guild, channel, musicManager, track);
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "spotify.added", track.getInfo().title))
                        .setColor(0x1DB954).build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    AudioTrack track = playlist.getTracks().get(0);
                    ctx.connectAndPlay(guild, channel, musicManager, track);
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription(Lang.t(gid, "spotify.added", track.getInfo().title))
                            .setColor(0x1DB954).build()).queue();
                }
            }

            @Override
            public void noMatches() {
                event.getHook().sendMessage(Lang.t(gid, "spotify.not.on.youtube")).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getHook().sendMessage(Lang.t(gid, "error.generic", e.getMessage())).queue();
            }
        });
    }

    private void loadSpotifyMultiple(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel,
                                     GuildMusicManager musicManager, java.util.List<String> searches, long gid) {
        final int[] loaded = {0};
        final int[] failed = {0};
        final int total = searches.size();
        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "spotify.loading", total))
                .setColor(0x1DB954).build()).queue();
        for (String search : searches) {
            ctx.playerManager.loadItemOrdered(musicManager, search, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    ctx.connectAndPlay(guild, channel, musicManager, track);
                    loaded[0]++;
                    checkDone();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    if (!playlist.getTracks().isEmpty()) {
                        ctx.connectAndPlay(guild, channel, musicManager, playlist.getTracks().get(0));
                        loaded[0]++;
                    }
                    checkDone();
                }

                @Override
                public void noMatches() {
                    failed[0]++;
                    checkDone();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    log.warn("[Spotify] Load failed: {} - {}", search, e.getMessage());
                    failed[0]++;
                    checkDone();
                }

                private void checkDone() {
                    if (loaded[0] + failed[0] >= total) {
                        String msg = Lang.t(gid, "spotify.loaded", loaded[0], total);
                        if (failed[0] > 0) msg += Lang.t(gid, "spotify.failed.count", failed[0]);
                        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                .setDescription(msg).setColor(0x1DB954).build()).queue();
                    }
                }
            });
        }
    }

    private void loadAndPlay(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel,
                             GuildMusicManager musicManager, String query, boolean forcePlay) {
        long gid = guild.getIdLong();
        ctx.playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                boolean duplicate = !forcePlay && musicManager.scheduler.isDuplicate(track.getInfo().uri);
                ctx.connectAndPlay(guild, channel, musicManager, track, forcePlay);
                String msg;
                int color;
                if (forcePlay) {
                    msg = Lang.t(gid, "radio.started", track.getInfo().title);
                    color = 0xEB459E;
                } else if (duplicate) {
                    msg = Lang.t(gid, "track.duplicate", track.getInfo().title);
                    color = 0xFEE75C;
                } else {
                    msg = Lang.t(gid, "track.added", track.getInfo().title);
                    color = 0x5865F2;
                }
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setDescription(msg).setColor(color).build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack track = playlist.getTracks().get(0);
                    ctx.connectAndPlay(guild, channel, musicManager, track);
                    event.getHook().sendMessage(Lang.t(gid, "track.added", track.getInfo().title)).queue();
                } else {
                    boolean first = true;
                    for (AudioTrack track : playlist.getTracks()) {
                        if (first) {
                            ctx.connectAndPlay(guild, channel, musicManager, track);
                            first = false;
                        } else {
                            musicManager.scheduler.queue(track);
                        }
                    }
                    event.getHook().sendMessage(Lang.t(gid, "playlist.added", playlist.getTracks().size(), playlist.getName())).queue();
                }
            }

            @Override
            public void noMatches() {
                OptionMapping qOpt = event.getOption("query");
                String displayQuery = qOpt != null ? qOpt.getAsString() : query;
                event.getHook().sendMessage(Lang.t(gid, "nothing.found", displayQuery)).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                log.error("Load failed: {}", e.getMessage(), e);
                event.getHook().sendMessage(Lang.t(gid, "load.error", e.getMessage())).queue();
            }
        });
    }

    public void handleSkip(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        AudioTrack next = manager.scheduler.peek();
        boolean nonstop = ctx.nonstopGuilds.contains(gid);
        manager.scheduler.skip();
        EmbedBuilder embed = new EmbedBuilder().setColor(0x5865F2);
        if (next != null) {
            embed.setDescription(Lang.t(gid, "skip.next", next.getInfo().title));
        } else if (nonstop) {
            embed.setDescription(Lang.t(gid, "skip.nonstop"));
        } else {
            embed.setDescription(Lang.t(gid, "skip.empty"));
        }
        event.replyEmbeds(embed.build()).queue();
    }

    public void handleStop(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        ctx.nonstopGuilds.remove(guildId);
        manager.scheduler.stop();
        NonstopHandler.installIdleHandler(ctx, guildId, manager);
        NonstopHandler.scheduleAutoNonstop(ctx, guildId, manager);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "stop.full"))
                .setColor(0xED4245).build()).queue();
    }

    public void handlePause(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        manager.player.setPaused(true);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = Lang.t(gid, "pause");
        if (track != null) desc += " \u2014 **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0xFEE75C).build()).queue();
    }

    public void handleResume(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        manager.player.setPaused(false);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = Lang.t(gid, "resume");
        if (track != null) desc += " \u2014 **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0x57F287).build()).queue();
    }

    public void handleSeek(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = ctx.getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();
        if (track == null) {
            event.reply(Lang.t(gid, "nothing.playing")).setEphemeral(true).queue();
            return;
        }
        OptionMapping timeOpt = event.getOption("time");
        if (timeOpt == null) {
            event.reply(Lang.t(gid, "seek.invalid")).setEphemeral(true).queue();
            return;
        }
        String input = timeOpt.getAsString().trim();
        long ms = parseTimeToMs(input);
        if (ms < 0) {
            event.reply(Lang.t(gid, "seek.invalid")).setEphemeral(true).queue();
            return;
        }
        if (ms > track.getDuration()) ms = track.getDuration();
        track.setPosition(ms);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "seek.success", ctx.formatTime(ms), ctx.formatTime(track.getDuration())))
                .setColor(0x5865F2).build()).queue();
    }

    private long parseTimeToMs(String input) {
        if (input.contains(":")) {
            String[] parts = input.split(":");
            try {
                if (parts.length == 2) {
                    return (Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1])) * 1000;
                } else if (parts.length == 3) {
                    return (Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2])) * 1000;
                }
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        try {
            return Long.parseLong(input) * 1000;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
