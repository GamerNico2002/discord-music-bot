package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CommandHandler extends ListenerAdapter {

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    private final String ownerId;
    private final String ownerDisplay;
    private final String supportContact;
    private final SpotifyResolver spotify;

    private static final Map<String, String[]> RADIO_STATIONS = new java.util.LinkedHashMap<>() {{
        put("1live",      new String[]{"\uD83D\uDCFB 1LIVE", "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3"});
        put("wdr2",       new String[]{"\uD83D\uDCFB WDR 2", "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3"});
        put("swr3",       new String[]{"\uD83D\uDCFB SWR3", "https://liveradio.swr.de/sw282p3/swr3/play.mp3"});
        put("bayern3",    new String[]{"\uD83D\uDCFB Bayern 3", "https://streams.br.de/bayern3_2.m3u"});
        put("bigfm",      new String[]{"\uD83D\uDCFB bigFM", "https://streams.bigfm.de/bigfm-deutschland-128-mp3"});
        put("radiobob",   new String[]{"\uD83C\uDFA4 Radio BOB!", "https://streams.radiobob.de/bob-live/mp3-192/mediaplayer"});
        put("nrj",        new String[]{"\uD83D\uDCFB Energy/NRJ", "https://frontend.streamonkey.net/energy-madeingermany/stream/mp3"});
        put("antenne",    new String[]{"\uD83D\uDCFB Antenne Bayern", "https://stream.antenne.de/antenne/stream/mp3"});
        put("jump",       new String[]{"\uD83D\uDCFB MDR JUMP", "https://mdr-284320-0.sslcast.mdr.de/mdr/284320/0/mp3/high/stream.mp3"});
        put("sunshine",   new String[]{"\u2600\uFE0F Sunshine Live", "https://stream.sunshine-live.de/live/mp3-192/stream"});
        put("jamfm",      new String[]{"\uD83C\uDFB5 JAM FM", "https://stream.jam.fm/jamfm-live/mp3-192/mediaplayer"});
        put("hitrtl",     new String[]{"\uD83D\uDCFB HITRADIO RTL", "https://web.radio.hitradio-rtl.de/hrrtl-sachsen/stream/mp3"});
        put("89rtl",      new String[]{"\uD83D\uDCFB 89.0 RTL", "https://stream.89.0rtl.de/live/mp3-192/stream"});
        put("lausitz",    new String[]{"\uD83D\uDCFB Radio Lausitz", "https://web.radio.radiolausitz.de/radiolausitz-live/stream/mp3"});
        put("radiopsr",   new String[]{"\uD83D\uDCFB Radio PSR", "https://streams.radiopsr.de/psr-live/mp3-192/stream"});
        put("rsa",        new String[]{"\uD83D\uDCFB R.SA", "https://streams.rsa-sachsen.de/rsa-live/mp3-192/stream"});
        put("rtl",        new String[]{"\uD83D\uDCFB RTL Radio", "https://stream.rtlradio.de/rtl-de/mp3-192/stream"});
        put("lofi",       new String[]{"\uD83C\uDFB5 Lofi Hip Hop", "https://play.streamafrica.net/lofiradio"});
        put("chillhop",   new String[]{"\uD83C\uDFB6 Chillhop", "http://streams.fluxfm.de/Chillhop/mp3-320/audio/"});
        put("rock",       new String[]{"\uD83E\uDD18 Rock Hits", "https://streams.radiobob.de/bob-rockhits/mp3-192/mediaplayer"});
        put("schlager",   new String[]{"\uD83C\uDFB6 Schlager Radio", "https://streams.radiobob.de/bob-schlager/mp3-192/mediaplayer"});
        put("80er",       new String[]{"\uD83D\uDD7A 80er Hits", "https://streams.bigfm.de/bigfm-80er-128-mp3"});
        put("90er",       new String[]{"\uD83D\uDC83 90er Hits", "https://streams.bigfm.de/bigfm-90er-128-mp3"});
        put("freshhappywave", new String[]{"\uD83C\uDF1F Fresh Happy Wave", "https://laut.fm/freshhappywave"});
    }};
    private final java.util.Set<Long> nonstopGuilds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<Long> autoNonstopDisabledGuilds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final long AUTO_NONSTOP_DELAY_MS = 2 * 60 * 1000L;
    private final Map<Long, ScheduledFuture<?>> autoNonstopTimers = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String[] DEFAULT_NONSTOP_GENRES = {
            "tekk", "hardtekk", "techno", "uptempo", "uptempo hardcore",
            "frenchcore", "hardstyle", "raw hardstyle", "rawstyle",
            "schranz", "hardcore", "minimal techno", "acid techno",
            "psytrance", "rave", "hard techno",
            "rock", "classic rock", "hard rock", "alternative rock",
            "rock hits", "deutschrock",
            "schlager", "schlager hits", "deutsche schlager", "schlager party"
    };
    private static final String[] DEFAULT_NONSTOP_MODIFIERS = {
            "song", "official audio", "official video", "hit",
            "2024", "2025", "best of", "remix"
    };
    private final String[] NONSTOP_GENRES;
    private final String[] NONSTOP_MODIFIERS;
    private final java.util.Random nonstopRandom = new java.util.Random();
    private final Map<Long, Long> lastChannelIds = new HashMap<>();
    private final ScheduledExecutorService npScheduler = Executors.newScheduledThreadPool(32);
    private final Map<Long, ScheduledFuture<?>> npUpdateTasks = new HashMap<>();
    private final Map<Long, net.dv8tion.jda.api.interactions.InteractionHook> npHooks = new HashMap<>();

    public CommandHandler() {
        var cfg = MusicBot.CONFIG;
        spotify = new SpotifyResolver(
                cfg.getProperty("spotify.client.id", ""),
                cfg.getProperty("spotify.client.secret", ""));
        ownerId = cfg.getProperty("bot.owner.id", "").trim();
        ownerDisplay = cfg.getProperty("bot.owner", "Unbekannt");
        supportContact = cfg.getProperty("bot.support", "Keine Angabe");
        playerManager.getConfiguration().setOpusEncodingQuality(5);
        playerManager.getConfiguration().setResamplingQuality(com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality.MEDIUM);
        playerManager.setFrameBufferDuration(300);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(playerManager);
        NONSTOP_GENRES = loadCsvProperty("nonstop.genres", DEFAULT_NONSTOP_GENRES);
        NONSTOP_MODIFIERS = loadCsvProperty("nonstop.modifiers", DEFAULT_NONSTOP_MODIFIERS);
        System.out.println("[Nonstop] " + NONSTOP_GENRES.length + " Genres, " + NONSTOP_MODIFIERS.length + " Modifiers geladen");
    }

    private String[] loadCsvProperty(String key, String[] fallback) {
        String value = MusicBot.CONFIG.getProperty(key, "").trim();
        if (value.isBlank()) return fallback;
        String[] parts = value.split("\\s*,\\s*");
        long nonEmpty = java.util.Arrays.stream(parts).filter(p -> !p.isBlank()).count();
        if (nonEmpty == 0) return fallback;
        String[] result = new String[(int) nonEmpty];
        int idx = 0;
        for (String p : parts) if (!p.isBlank()) result[idx++] = p.trim();
        return result;
    }

    private GuildMusicManager getGuildMusic(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
            var manager = new GuildMusicManager(playerManager);
            manager.scheduler.setJda(guild.getJDA());
            manager.scheduler.setGuildId(guild.getIdLong());
            guild.getAudioManager().setSendingHandler(manager.sendHandler);
            return manager;
        });
    }

    private void cleanupGuild(long guildId) {
        System.out.println("[Cleanup] Guild " + guildId + " - alle Daten entfernen");
        cancelAutoNonstop(guildId);
        nonstopGuilds.remove(guildId);
        lastChannelIds.remove(guildId);
        GuildMusicManager manager = musicManagers.remove(guildId);
        if (manager != null) {
            try { manager.scheduler.setOnIdle(null); } catch (Exception ignored) {}
            try { manager.scheduler.stop(); } catch (Exception ignored) {}
        }
        ScheduledFuture<?> nps = npUpdateTasks.remove(guildId);
        if (nps != null) nps.cancel(false);
        npHooks.remove(guildId);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        // Bot selbst wurde verschoben/disconnected
        if (event.getMember().getUser().equals(event.getJDA().getSelfUser())) {
            if (event.getChannelJoined() != null) {
                lastChannelIds.put(guildId, event.getChannelJoined().getIdLong());
                System.out.println("[Voice] Bot joined: " + event.getChannelJoined().getName());
            }

            if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
                System.out.println("[Voice] Bot wurde disconnected von: " + event.getChannelLeft().getName());
                Long channelId = lastChannelIds.get(guildId);
                if (channelId != null && musicManagers.containsKey(guildId)) {
                    AudioChannel channel = guild.getVoiceChannelById(channelId);
                    if (channel == null) channel = guild.getStageChannelById(channelId);
                    if (channel != null) {
                        System.out.println("[Voice] Auto-Reconnect zu: " + channel.getName());
                        GuildMusicManager manager = musicManagers.get(guildId);
                        AudioManager audioManager = guild.getAudioManager();
                        audioManager.setSendingHandler(manager.sendHandler);
                        audioManager.setSelfDeafened(true);
                        audioManager.openAudioConnection(channel);
                        installNonstopIdleHandler(guildId, manager);
                        scheduleAutoNonstop(guildId, manager);
                    } else {
                        // Channel existiert nicht mehr -> Cleanup
                        cleanupGuild(guildId);
                    }
                } else {
                    cleanupGuild(guildId);
                }
            }
            return;
        }

        // Anderer User hat den Channel verlassen — Bot bleibt und spielt weiter
        // Nur /leave trennt die Verbindung
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        switch (event.getName()) {
            case "play" -> handlePlay(event);
            case "skip" -> handleSkip(event);
            case "stop" -> handleStop(event);
            case "pause" -> handlePause(event);
            case "resume" -> handleResume(event);
            case "queue" -> handleQueue(event);
            case "playing" -> handleNowPlaying(event);
            case "volume" -> handleVolume(event);
            case "join" -> handleJoin(event);
            case "leave" -> handleLeave(event);
            case "repeat" -> handleRepeat(event);
            case "shuffle" -> handleShuffle(event);
            case "radio" -> handleRadio(event);
            case "seek" -> handleSeek(event);
            case "remove" -> handleRemove(event);
            case "clear" -> handleClear(event);
            case "move" -> handleMove(event);
            case "skipto" -> handleSkipTo(event);
            case "save" -> handleSave(event);
            case "nonstop" -> handle247(event);
            case "filter" -> handleFilter(event);
            case "invite" -> handleInvite(event);
            case "help" -> handleHelp(event);
            case "info" -> handleInfo(event);
            case "ping" -> handlePing(event);
            case "dcleave" -> handleDcLeave(event);
            case "language" -> handleLanguage(event);
        }
    }

    private void handleLanguage(SlashCommandInteractionEvent event) {
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
        event.replyEmbeds(new EmbedBuilder()
                .setTitle(Lang.t(guildId, "lang.title"))
                .setDescription(Lang.t(guildId, "lang.changed", label))
                .setColor(0x57F287).build()).queue();
    }

    private void handleDcLeave(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        if (ownerId.isEmpty() || !event.getUser().getId().equals(ownerId)) {
            event.reply(Lang.t(gid, "dcleave.not.owner")).setEphemeral(true).queue();
            return;
        }
        String guildId = event.getOption("server").getAsString();
        Guild target = event.getJDA().getGuildById(guildId);
        if (target == null) {
            event.reply(Lang.t(gid, "dcleave.not.found", guildId)).setEphemeral(true).queue();
            return;
        }
        String name = target.getName();
        event.reply(Lang.t(gid, "dcleave.leaving", name, guildId)).setEphemeral(true).queue();
        target.leave().queue(
                s -> System.out.println("[dcleave] Server verlassen: " + name + " (" + guildId + ")"),
                err -> System.err.println("[dcleave] Fehler beim Verlassen von " + name + ": " + err.getMessage())
        );
    }

    private void handlePlay(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }

        String query = event.getOption("query").getAsString();

        event.deferReply().queue();

        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = getGuildMusic(guild);

        // Stoppe Nonstop wenn aktiv - User-Song soll Vorrang haben
        boolean wasNonstop = nonstopGuilds.remove(guildId);
        cancelAutoNonstop(guildId);
        if (wasNonstop) {
            musicManager.scheduler.clearQueue();
            musicManager.player.stopTrack();
            System.out.println("[Nonstop] Gestoppt durch /play in guild " + guildId);
        }

        if (spotify.isSpotifyUrl(query)) {
            handleSpotifyPlay(event, guild, channel, musicManager, query);
            return;
        }

        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            query = "ytsearch:" + query;
        }

        loadAndPlay(event, guild, channel, musicManager, query, false);
    }

    private void loadAndPlay(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager, String query, boolean forcePlay) {
        long gid = guild.getIdLong();
        playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                boolean duplicate = !forcePlay && musicManager.scheduler.isDuplicate(track.getInfo().uri);
                connectAndPlay(guild, channel, musicManager, track, forcePlay);
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
                    connectAndPlay(guild, channel, musicManager, track);
                    event.getHook().sendMessage(Lang.t(gid, "track.added", track.getInfo().title)).queue();
                } else {
                    boolean first = true;
                    for (AudioTrack track : playlist.getTracks()) {
                        if (first) {
                            connectAndPlay(guild, channel, musicManager, track);
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
                event.getHook().sendMessage(Lang.t(gid, "nothing.found", event.getOption("query").getAsString())).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getHook().sendMessage(Lang.t(gid, "load.error", e.getMessage())).queue();
                System.err.println("Load failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleSpotifyPlay(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager, String url) {
        long gid = guild.getIdLong();
        if (!spotify.isConfigured()) {
            event.getHook().sendMessage(Lang.t(gid, "spotify.not.configured")).queue();
            return;
        }
        spotify.resolveAsync(url).thenAcceptAsync(searches -> {
            if (searches.isEmpty()) {
                event.getHook().sendMessage(Lang.t(gid, "spotify.no.songs")).queue();
                return;
            }
            if (searches.size() == 1) {
                playerManager.loadItemOrdered(musicManager, searches.get(0), new AudioLoadResultHandler() {
                    @Override public void trackLoaded(AudioTrack track) {
                        connectAndPlay(guild, channel, musicManager, track);
                        event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                .setDescription(Lang.t(gid, "spotify.added", track.getInfo().title))
                                .setColor(0x1DB954).build()).queue();
                    }
                    @Override public void playlistLoaded(AudioPlaylist playlist) {
                        if (!playlist.getTracks().isEmpty()) {
                            AudioTrack track = playlist.getTracks().get(0);
                            connectAndPlay(guild, channel, musicManager, track);
                            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                    .setDescription(Lang.t(gid, "spotify.added", track.getInfo().title))
                                    .setColor(0x1DB954).build()).queue();
                        }
                    }
                    @Override public void noMatches() { event.getHook().sendMessage(Lang.t(gid, "spotify.not.on.youtube")).queue(); }
                    @Override public void loadFailed(FriendlyException e) { event.getHook().sendMessage(Lang.t(gid, "error.generic", e.getMessage())).queue(); }
                });
            } else {
                final int[] loaded = {0};
                final int[] failed = {0};
                final int total = searches.size();
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "spotify.loading", total))
                        .setColor(0x1DB954).build()).queue();
                for (String search : searches) {
                    playerManager.loadItemOrdered(musicManager, search, new AudioLoadResultHandler() {
                        @Override public void trackLoaded(AudioTrack track) {
                            connectAndPlay(guild, channel, musicManager, track);
                            loaded[0]++;
                            checkDone();
                        }
                        @Override public void playlistLoaded(AudioPlaylist playlist) {
                            if (!playlist.getTracks().isEmpty()) {
                                connectAndPlay(guild, channel, musicManager, playlist.getTracks().get(0));
                                loaded[0]++;
                            }
                            checkDone();
                        }
                        @Override public void noMatches() {
                            failed[0]++;
                            checkDone();
                        }
                        @Override public void loadFailed(FriendlyException e) {
                            System.err.println("[Spotify] Load failed: " + search + " - " + e.getMessage());
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
        }, npScheduler).exceptionally(e -> {
            event.getHook().sendMessage(Lang.t(gid, "spotify.error", e.getCause().getMessage())).queue();
            return null;
        });
    }

    private void handleRadio(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }
        String key = event.getOption("sender").getAsString().toLowerCase();
        String[] station = RADIO_STATIONS.get(key);
        if (station == null) {
            event.reply(Lang.t(gid, "radio.unknown")).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();
        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusic(guild);

        playerManager.loadItemOrdered(musicManager, station[1], new AudioLoadResultHandler() {
            @Override public void trackLoaded(AudioTrack track) {
                connectAndPlay(guild, channel, musicManager, track, true);
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(gid, "radio.started", station[0]))
                        .setColor(0xEB459E).build()).queue();
            }
            @Override public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    connectAndPlay(guild, channel, musicManager, playlist.getTracks().get(0), true);
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription(Lang.t(gid, "radio.started", station[0]))
                            .setColor(0xEB459E).build()).queue();
                }
            }
            @Override public void noMatches() { event.getHook().sendMessage(Lang.t(gid, "radio.unreachable")).queue(); }
            @Override public void loadFailed(FriendlyException e) { event.getHook().sendMessage(Lang.t(gid, "error.generic", e.getMessage())).queue(); }
        });
    }

    private static final Map<String, String> FILTER_PRESETS = new java.util.LinkedHashMap<>() {{
        put("bassboost", "\uD83D\uDD0A Bassboost");
        put("treble", "🎵 Treble Boost");
        put("pop", "🎤 Pop");
        put("rock", "🤘 Rock");
        put("off", "➡️ Aus");
    }};

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String input = event.getFocusedOption().getValue().toLowerCase();
        List<Command.Choice> choices = new ArrayList<>();

        if (event.getName().equals("radio") && event.getFocusedOption().getName().equals("sender")) {
            for (var entry : RADIO_STATIONS.entrySet()) {
                if (choices.size() >= 25) break;
                String key = entry.getKey();
                String display = entry.getValue()[0];
                if (input.isBlank() || key.contains(input) || display.toLowerCase().contains(input)) {
                    choices.add(new Command.Choice(display, key));
                }
            }
        } else if (event.getName().equals("filter") && event.getFocusedOption().getName().equals("preset")) {
            for (var entry : FILTER_PRESETS.entrySet()) {
                if (input.isBlank() || entry.getKey().contains(input) || entry.getValue().toLowerCase().contains(input)) {
                    choices.add(new Command.Choice(entry.getValue(), entry.getKey()));
                }
            }
        } else if (event.getName().equals("dcleave") && event.getFocusedOption().getName().equals("server")) {
            String userId = event.getUser().getId();
            System.out.println("[dcleave] Autocomplete von User " + userId + " (Owner=" + ownerId + ")");
            if (ownerId.isEmpty() || !userId.equals(ownerId)) {
                System.out.println("[dcleave] Nicht-Owner -> keine Auswahl");
                event.replyChoices(List.of()).queue();
                return;
            }
            for (Guild g : event.getJDA().getGuilds()) {
                if (choices.size() >= 25) break;
                String name = g.getName();
                String id = g.getId();
                if (!input.isBlank() && !name.toLowerCase().contains(input) && !id.contains(input)) continue;
                // Discord-Limit: Choice-Name max 100 Zeichen
                String suffix = " (" + id + ")";
                int maxNameLen = 100 - suffix.length();
                String label = name.length() > maxNameLen ? name.substring(0, maxNameLen - 3) + "..." : name;
                choices.add(new Command.Choice(label + suffix, id));
            }
            System.out.println("[dcleave] " + choices.size() + " Server-Vorschlaege");
        }

        event.replyChoices(choices).queue();
    }

    private void connectAndPlay(Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager, AudioTrack track) {
        connectAndPlay(guild, channel, musicManager, track, false);
    }

    private void connectAndPlay(Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager, AudioTrack track, boolean forcePlay) {
        long guildId = guild.getIdLong();
        lastChannelIds.put(guildId, channel.getIdLong());
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);
        }
        installNonstopIdleHandler(guildId, musicManager);
        cancelAutoNonstop(guildId);
        if (forcePlay) {
            musicManager.scheduler.playNow(track);
        } else {
            musicManager.scheduler.queue(track);
        }
    }

    private void installNonstopIdleHandler(long guildId, GuildMusicManager musicManager) {
        musicManager.scheduler.setOnIdle(() -> {
            if (nonstopGuilds.contains(guildId)) {
                queueRandomNonstopTrack(guildId, musicManager);
            } else {
                scheduleAutoNonstop(guildId, musicManager);
            }
        });
    }

    private void scheduleAutoNonstop(long guildId, GuildMusicManager musicManager) {
        cancelAutoNonstop(guildId);
        if (autoNonstopDisabledGuilds.contains(guildId)) {
            System.out.println("[AutoNonstop] Deaktiviert fuer guild " + guildId);
            return;
        }
        System.out.println("[AutoNonstop] Idle timer (2min) gestartet fuer guild " + guildId);
        ScheduledFuture<?> task = npScheduler.schedule(() -> {
            try {
                if (musicManager.player.getPlayingTrack() != null) return;
                if (!musicManager.scheduler.getQueue().isEmpty()) return;
                Guild guild = MusicBot.JDA != null ? MusicBot.JDA.getGuildById(guildId) : null;
                if (guild == null || !guild.getAudioManager().isConnected()) {
                    System.out.println("[AutoNonstop] Guild nicht verbunden -> cleanup");
                    cleanupGuild(guildId);
                    return;
                }
                if (autoNonstopDisabledGuilds.contains(guildId)) return;
                System.out.println("[AutoNonstop] 2min Idle - starte Nonstop fuer guild " + guildId);
                nonstopGuilds.add(guildId);
                installNonstopIdleHandler(guildId, musicManager);
                queueRandomNonstopTrack(guildId, musicManager);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, AUTO_NONSTOP_DELAY_MS, TimeUnit.MILLISECONDS);
        autoNonstopTimers.put(guildId, task);
    }

    private void cancelAutoNonstop(long guildId) {
        ScheduledFuture<?> t = autoNonstopTimers.remove(guildId);
        if (t != null) t.cancel(false);
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack next = manager.scheduler.peek();
        boolean nonstop = nonstopGuilds.contains(gid);
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

    private void handleStop(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        nonstopGuilds.remove(guildId);
        manager.scheduler.stop();
        installNonstopIdleHandler(guildId, manager);
        scheduleAutoNonstop(guildId, manager);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "stop.full"))
                .setColor(0xED4245).build()).queue();
    }

    private void handlePause(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setPaused(true);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = Lang.t(gid, "pause");
        if (track != null) desc += " \u2014 **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0xFEE75C).build()).queue();
    }

    private void handleResume(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setPaused(false);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = Lang.t(gid, "resume");
        if (track != null) desc += " \u2014 **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0x57F287).build()).queue();
    }

    private static final int QUEUE_PAGE_SIZE = 10;

    private void handleQueue(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        AudioTrack current = manager.player.getPlayingTrack();

        if (tracks.isEmpty() && current == null) {
            event.reply(Lang.t(gid, "queue.empty")).queue();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(tracks.size() / (double) QUEUE_PAGE_SIZE));
        event.replyEmbeds(buildQueueEmbed(gid, tracks, current, manager, 0, totalPages).build())
                .addComponents(ActionRow.of(
                        Button.secondary("queue_prev_0", Emoji.fromUnicode("◀️")).withDisabled(true),
                        Button.secondary("queue_page", Lang.t(gid, "page") + " 1/" + totalPages).withDisabled(true),
                        Button.secondary("queue_next_0", Emoji.fromUnicode("▶️")).withDisabled(totalPages <= 1)
                )).queue();
    }

    private EmbedBuilder buildQueueEmbed(long gid, List<AudioTrack> tracks, AudioTrack current, GuildMusicManager manager, int page, int totalPages) {
        StringBuilder sb = new StringBuilder();

        if (current != null && page == 0) {
            sb.append(Lang.t(gid, "now.playing.now")).append("\n")
              .append("`").append(formatTime(current.getPosition())).append(" / ").append(formatTime(current.getDuration())).append("` ")
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
                  .append(" `[").append(formatTime(track.getDuration())).append("]`\n");
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
                .setFooter(Lang.t(gid, "queue.footer", tracks.size(), formatTimeLong(totalMs), repeatLabel))
                .setColor(0x5865F2);
    }

    private String formatTimeLong(long ms) {
        long hours = ms / 3600000;
        long mins = (ms % 3600000) / 60000;
        long secs = (ms % 60000) / 1000;
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m " + secs + "s";
    }

    private void handleNowPlaying(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();
        long guildId = event.getGuild().getIdLong();
        boolean nonstop = nonstopGuilds.contains(guildId);

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
                .queue(hook -> startNpAutoUpdate(guildId, manager, event.getHook()));
    }

    private EmbedBuilder buildLoadingEmbed(long gid) {
        return new EmbedBuilder()
                .setTitle(Lang.t(gid, "now.playing.title"))
                .setDescription(Lang.t(gid, "nonstop.loading"))
                .setColor(0x5865F2);
    }

    private void startNpAutoUpdate(long guildId, GuildMusicManager manager, net.dv8tion.jda.api.interactions.InteractionHook hook) {
        ScheduledFuture<?> oldTask = npUpdateTasks.remove(guildId);
        if (oldTask != null) oldTask.cancel(false);
        npHooks.put(guildId, hook);
        ScheduledFuture<?> task = npScheduler.scheduleAtFixedRate(() -> {
            try {
                if (npHooks.get(guildId) != hook) return;
                AudioTrack current = manager.player.getPlayingTrack();
                boolean nonstop = nonstopGuilds.contains(guildId);
                if (current == null && !nonstop) return;
                EmbedBuilder embed = current != null
                        ? buildNowPlayingEmbed(guildId, current, manager)
                        : buildLoadingEmbed(guildId);
                hook.editOriginalEmbeds(embed.build())
                        .setComponents(npButtons(manager)).queue(null, err -> {});
            } catch (Exception ignored) {}
        }, 3, 3, TimeUnit.SECONDS);
        npUpdateTasks.put(guildId, task);
    }

    private void cancelNpUpdate(long guildId) {
        ScheduledFuture<?> task = npUpdateTasks.remove(guildId);
        if (task != null) task.cancel(false);
        var oldHook = npHooks.remove(guildId);
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
        String position = formatTime(pos) + " / " + formatTime(dur);
        String progressBar = buildProgressBar(pos, dur);
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
                        "`" + position + "`  \u23F3 `-" + formatTime(remaining) + "`" + repeatLabel + "\n\n" +
                        status)
                .setColor(0x5865F2)
                .setThumbnail("https://img.youtube.com/vi/" + extractVideoId(track.getInfo().uri) + "/0.jpg");
    }

    private String buildProgressBar(long position, long duration) {
        int total = 20;
        int filled = duration > 0 ? (int) (position * total / duration) : 0;
        return "▬".repeat(filled) + "\uD83D\uDD18" + "▬".repeat(Math.max(0, total - filled - 1));
    }

    private String extractVideoId(String url) {
        if (url == null) return "";
        if (url.contains("v=")) return url.substring(url.indexOf("v=") + 2).split("[&?]")[0];
        if (url.contains("youtu.be/")) return url.substring(url.indexOf("youtu.be/") + 9).split("[&?]")[0];
        return "";
    }

    private ActionRow npButtons(GuildMusicManager manager) {
        return ActionRow.of(
                Button.secondary("np_restart", Emoji.fromUnicode("⏮")),
                Button.primary("np_pause", Emoji.fromUnicode(manager.player.isPaused() ? "▶️" : "⏸️")),
                Button.secondary("np_skip", Emoji.fromUnicode("⏭")),
                Button.danger("np_stop", Emoji.fromUnicode("⏹️"))
        );
    }

    private String formatTime(long ms) {
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return mins + ":" + String.format("%02d", secs);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        String id = event.getComponentId();

        if (id.startsWith("queue_")) {
            handleQueueButton(event);
            return;
        }

        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();

        long guildId = event.getGuild().getIdLong();

        switch (id) {
            case "np_restart" -> {
                if (track != null) track.setPosition(0);
                AudioTrack t = track != null ? track : manager.player.getPlayingTrack();
                if (t != null) {
                    event.editMessageEmbeds(buildNowPlayingEmbed(guildId, t, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                }
            }
            case "np_pause" -> {
                manager.player.setPaused(!manager.player.isPaused());
                track = manager.player.getPlayingTrack();
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
                } else if (nonstopGuilds.contains(guildId)) {
                    event.editMessageEmbeds(buildLoadingEmbed(guildId).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else {
                    cancelNpUpdate(guildId);
                    event.editMessage(Lang.t(guildId, "skipped.empty.short")).setComponents().setEmbeds().queue();
                }
            }
            case "np_stop" -> {
                cancelNpUpdate(guildId);
                nonstopGuilds.remove(guildId);
                manager.scheduler.stop();
                installNonstopIdleHandler(guildId, manager);
                scheduleAutoNonstop(guildId, manager);
                event.editMessage(Lang.t(guildId, "stop.short")).setComponents().setEmbeds().queue();
            }
        }
    }

    private void handleQueueButton(ButtonInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
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

        event.editMessageEmbeds(buildQueueEmbed(gid, tracks, current, manager, currentPage, totalPages).build())
                .setComponents(ActionRow.of(
                        Button.secondary("queue_prev_" + currentPage, Emoji.fromUnicode("◀️")).withDisabled(currentPage == 0),
                        Button.secondary("queue_page", Lang.t(gid, "page") + " " + (currentPage + 1) + "/" + totalPages).withDisabled(true),
                        Button.secondary("queue_next_" + currentPage, Emoji.fromUnicode("▶️")).withDisabled(currentPage >= totalPages - 1)
                )).queue();
    }

    private void handleVolume(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        int vol = event.getOption("vol").getAsInt();
        if (vol < 0 || vol > 100) {
            event.reply(Lang.t(gid, "volume.range")).setEphemeral(true).queue();
            return;
        }
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setVolume(vol);
        String bar = buildVolumeBar(vol);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "volume.set", vol, bar))
                .setColor(0x5865F2).build()).queue();
    }

    private String buildVolumeBar(int vol) {
        int filled = vol / 5;
        return "▬".repeat(filled) + "\uD83D\uDD18" + "▬".repeat(20 - filled);
    }

    private void handleJoin(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(gid, "voice.required")).setEphemeral(true).queue();
            return;
        }
        AudioChannelUnion channel = voiceState.getChannel();
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = getGuildMusic(guild);
        lastChannelIds.put(guildId, channel.getIdLong());
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        audioManager.openAudioConnection(channel);
        installNonstopIdleHandler(guildId, musicManager);
        scheduleAutoNonstop(guildId, musicManager);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "joined", channel.getName()))
                .setColor(0x57F287).build()).queue();
    }

    private void handleLeave(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        nonstopGuilds.remove(guildId);
        cancelAutoNonstop(guildId);
        manager.scheduler.stop();
        lastChannelIds.remove(guildId);
        musicManagers.remove(guildId);
        event.getGuild().getAudioManager().closeAudioConnection();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "bye"))
                .setColor(0xED4245).build()).queue();
    }

    private void handleRepeat(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        String mode = event.getOption("mode").getAsString().toUpperCase();
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

    private void handleShuffle(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        if (manager.scheduler.getQueue().isEmpty()) {
            event.reply(Lang.t(gid, "queue.empty")).setEphemeral(true).queue();
            return;
        }
        manager.scheduler.shuffle();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "shuffled", manager.scheduler.getQueue().size()))
                .setColor(0x5865F2).build()).queue();
    }

    private void handleMove(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int from = event.getOption("von").getAsInt();
        int to = event.getOption("nach").getAsInt();
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

    private void handleSkipTo(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int pos = event.getOption("position").getAsInt();
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

    private void handleSave(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
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
                .setThumbnail("https://img.youtube.com/vi/" + extractVideoId(track.getInfo().uri) + "/0.jpg")
                .setColor(0x57F287);
        event.getUser().openPrivateChannel().queue(
                dm -> {
                    dm.sendMessageEmbeds(embed.build()).queue(
                            success -> event.reply(Lang.t(gid, "save.dm.sent")).setEphemeral(true).queue(),
                            fail -> event.reply(Lang.t(gid, "save.dm.failed")).setEphemeral(true).queue()
                    );
                },
                fail -> event.reply(Lang.t(gid, "save.dm.failed.short")).setEphemeral(true).queue()
        );
    }

    private void handle247(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusic(guild);

        // Subcommand: auto-on / auto-off
        var modusOpt = event.getOption("modus");
        if (modusOpt != null) {
            String modus = modusOpt.getAsString();
            if (modus.equals("auto-off")) {
                autoNonstopDisabledGuilds.add(guildId);
                cancelAutoNonstop(guildId);
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(guildId, "auto.nonstop.off"))
                        .setColor(0xED4245).build()).queue();
                return;
            } else if (modus.equals("auto-on")) {
                autoNonstopDisabledGuilds.remove(guildId);
                if (musicManagers.containsKey(guildId)) {
                    installNonstopIdleHandler(guildId, musicManager);
                    if (musicManager.player.getPlayingTrack() == null && musicManager.scheduler.getQueue().isEmpty()) {
                        scheduleAutoNonstop(guildId, musicManager);
                    }
                }
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription(Lang.t(guildId, "auto.nonstop.on"))
                        .setColor(0x57F287).build()).queue();
                return;
            }
        }

        if (nonstopGuilds.contains(guildId)) {
            nonstopGuilds.remove(guildId);
            installNonstopIdleHandler(guildId, musicManager);
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription(Lang.t(guildId, "nonstop.off"))
                    .setColor(0xED4245).build()).queue();
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply(Lang.t(guildId, "voice.required")).setEphemeral(true).queue();
            return;
        }
        AudioChannelUnion channel = voiceState.getChannel();

        nonstopGuilds.add(guildId);
        cancelAutoNonstop(guildId);
        installNonstopIdleHandler(guildId, musicManager);

        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);
        }
        lastChannelIds.put(guildId, channel.getIdLong());

        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(guildId, "nonstop.on"))
                .setColor(0x57F287).build()).queue();

        if (musicManager.player.getPlayingTrack() == null) {
            queueRandomNonstopTrack(guildId, musicManager);
        }
    }

    private void queueRandomNonstopTrack(long guildId, GuildMusicManager musicManager) {
        queueRandomNonstopTrack(guildId, musicManager, 0);
    }

    private boolean isLiveOrRecording(AudioTrack track) {
        if (track.getInfo().isStream) return true;
        String title = track.getInfo().title;
        if (title == null) return false;
        String t = title.toLowerCase();
        return t.contains("live") || t.contains("aufnahme") || t.contains("bootleg")
                || t.contains("concert") || t.contains("konzert");
    }

    private void queueRandomNonstopTrack(long guildId, GuildMusicManager musicManager, int attempt) {
        if (attempt >= 5) {
            System.err.println("[Nonstop] Konnte nach 5 Versuchen keinen Track laden - retry in 2 Min");
            // Nicht aufgeben: Nonstop-Flag aus, dann Auto-Timer setzt sich erneut
            nonstopGuilds.remove(guildId);
            scheduleAutoNonstop(guildId, musicManager);
            return;
        }
        String genre = NONSTOP_GENRES[nonstopRandom.nextInt(NONSTOP_GENRES.length)];
        String modifier = NONSTOP_MODIFIERS[nonstopRandom.nextInt(NONSTOP_MODIFIERS.length)];
        String query = "ytsearch:" + genre + " " + modifier;
        System.out.println("[Nonstop] Suche: " + query);

        playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> filtered = new ArrayList<>();
                for (AudioTrack t : playlist.getTracks()) {
                    if (!isLiveOrRecording(t)) filtered.add(t);
                    if (filtered.size() >= 10) break;
                }
                if (filtered.isEmpty()) {
                    queueRandomNonstopTrack(guildId, musicManager, attempt + 1);
                    return;
                }
                AudioTrack pick = filtered.get(nonstopRandom.nextInt(filtered.size()));
                musicManager.scheduler.queue(pick);
            }

            @Override
            public void noMatches() {
                queueRandomNonstopTrack(guildId, musicManager, attempt + 1);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.err.println("[Nonstop] Load failed: " + e.getMessage());
                queueRandomNonstopTrack(guildId, musicManager, attempt + 1);
            }
        });
    }

    private void handleFilter(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        String filter = event.getOption("preset").getAsString().toLowerCase();
        manager.applyFilter(filter);
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

    private void handleSeek(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();
        if (track == null) {
            event.reply(Lang.t(gid, "nothing.playing")).setEphemeral(true).queue();
            return;
        }
        String input = event.getOption("time").getAsString().trim();
        long ms = parseTimeToMs(input);
        if (ms < 0) {
            event.reply(Lang.t(gid, "seek.invalid")).setEphemeral(true).queue();
            return;
        }
        if (ms > track.getDuration()) ms = track.getDuration();
        track.setPosition(ms);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(Lang.t(gid, "seek.success", formatTime(ms), formatTime(track.getDuration())))
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
            } catch (NumberFormatException e) { return -1; }
        }
        try {
            return Long.parseLong(input) * 1000;
        } catch (NumberFormatException e) { return -1; }
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int pos = event.getOption("position").getAsInt();
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

    private void handleClear(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        GuildMusicManager manager = getGuildMusic(event.getGuild());
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

    private void handleHelp(SlashCommandInteractionEvent event) {
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

    private void handleInvite(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        String botId = event.getJDA().getSelfUser().getId();
        String url = "https://discord.com/oauth2/authorize?client_id=" + botId + "&permissions=3145728&scope=bot%20applications.commands";
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "invite.title"))
                .setDescription(Lang.t(gid, "invite.desc", url))
                .setColor(0x57F287);
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleInfo(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();

        Duration uptime = Duration.between(MusicBot.START_TIME, Instant.now());
        String uptimeStr = formatUptime(uptime);

        long activePlayers = musicManagers.entrySet().stream()
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
                .addField(Lang.t(gid, "info.owner"), ownerDisplay, true)
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
                .addField(Lang.t(gid, "info.support"), supportContact, false)
                .addField(Lang.t(gid, "info.discord"), Lang.t(gid, "info.discord.value"), false)
                .setFooter(Lang.t(gid, "info.footer"));
        event.replyEmbeds(embed.build()).queue();
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long gid = event.getGuild().getIdLong();
        long gatewayPing = event.getJDA().getGatewayPing();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Lang.t(gid, "ping.title"))
                .setColor(0x5865F2)
                .addField(Lang.t(gid, "ping.gateway"), gatewayPing + "ms", true);
        event.replyEmbeds(embed.build()).queue();
    }


    private String formatUptime(Duration uptime) {
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
}
