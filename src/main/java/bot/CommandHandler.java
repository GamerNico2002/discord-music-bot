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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CommandHandler extends ListenerAdapter {

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    private final SpotifyResolver spotify = new SpotifyResolver(
            MusicBot.CONFIG.getProperty("spotify.client.id", ""),
            MusicBot.CONFIG.getProperty("spotify.client.secret", ""));

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
    private volatile net.dv8tion.jda.api.JDA jda;
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
    private final ScheduledExecutorService npScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, ScheduledFuture<?>> npUpdateTasks = new HashMap<>();
    private final Map<Long, net.dv8tion.jda.api.interactions.InteractionHook> npHooks = new HashMap<>();

    public CommandHandler() {
        playerManager.getConfiguration().setOpusEncodingQuality(5);
        playerManager.getConfiguration().setResamplingQuality(com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality.MEDIUM);
        playerManager.setFrameBufferDuration(1000);
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
        java.util.List<String> cleaned = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) cleaned.add(p.trim());
        return cleaned.isEmpty() ? fallback : cleaned.toArray(new String[0]);
    }

    private GuildMusicManager getGuildMusic(Guild guild) {
        if (jda == null) jda = guild.getJDA();
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
        ScheduledFuture<?> up = uptimeTasks.remove(guildId);
        if (up != null) up.cancel(false);
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
            case "uptime" -> handleUptime(event);
            case "ping" -> handlePing(event);
            case "dcleave" -> handleDcLeave(event);
        }
    }

    private void handleDcLeave(SlashCommandInteractionEvent event) {
        String ownerId = MusicBot.CONFIG.getProperty("bot.owner.id", "").trim();
        if (ownerId.isEmpty() || !event.getUser().getId().equals(ownerId)) {
            event.reply("\u26D4 Nur der Bot-Owner darf diesen Befehl benutzen.").setEphemeral(true).queue();
            return;
        }
        String guildId = event.getOption("server").getAsString();
        Guild target = event.getJDA().getGuildById(guildId);
        if (target == null) {
            event.reply("\u274C Server nicht gefunden (ID: " + guildId + ")").setEphemeral(true).queue();
            return;
        }
        String name = target.getName();
        event.reply("\uD83D\uDC4B Verlasse Server **" + name + "** (" + guildId + ")...").setEphemeral(true).queue();
        target.leave().queue(
                s -> System.out.println("[dcleave] Server verlassen: " + name + " (" + guildId + ")"),
                err -> System.err.println("[dcleave] Fehler beim Verlassen von " + name + ": " + err.getMessage())
        );
    }

    private void handlePlay(SlashCommandInteractionEvent event) {
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("Du musst in einem Voice-Channel sein!").setEphemeral(true).queue();
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
        playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                boolean duplicate = !forcePlay && musicManager.scheduler.isDuplicate(track.getInfo().uri);
                connectAndPlay(guild, channel, musicManager, track, forcePlay);
                String msg;
                int color;
                if (forcePlay) {
                    msg = "\uD83D\uDCFB **" + track.getInfo().title + "** gestartet";
                    color = 0xEB459E;
                } else if (duplicate) {
                    msg = "⚠️ **" + track.getInfo().title + "** ist bereits in der Queue! Trotzdem hinzugefuegt.";
                    color = 0xFEE75C;
                } else {
                    msg = "**" + track.getInfo().title + "** zur Queue hinzugefuegt";
                    color = 0x5865F2;
                }
                event.getHook().sendMessageEmbeds(new EmbedBuilder().setDescription(msg).setColor(color).build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack track = playlist.getTracks().get(0);
                    connectAndPlay(guild, channel, musicManager, track);
                    event.getHook().sendMessage("**" + track.getInfo().title + "** zur Queue hinzugefuegt").queue();
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
                    event.getHook().sendMessage("**" + playlist.getTracks().size() + " Songs** aus Playlist **" + playlist.getName() + "** hinzugefuegt").queue();
                }
            }

            @Override
            public void noMatches() {
                event.getHook().sendMessage("Nichts gefunden fuer: " + event.getOption("query").getAsString()).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.getHook().sendMessage("Fehler beim Laden: " + e.getMessage()).queue();
                System.err.println("Load failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleSpotifyPlay(SlashCommandInteractionEvent event, Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager, String url) {
        if (!spotify.isConfigured()) {
            event.getHook().sendMessage("Spotify ist nicht konfiguriert! Trage `spotify.client.id` und `spotify.client.secret` in config.properties ein.").queue();
            return;
        }
        npScheduler.execute(() -> {
            try {
                List<String> searches = spotify.resolve(url);
                if (searches.isEmpty()) {
                    event.getHook().sendMessage("Keine Songs in diesem Spotify-Link gefunden.").queue();
                    return;
                }
                if (searches.size() == 1) {
                    playerManager.loadItemOrdered(musicManager, searches.get(0), new AudioLoadResultHandler() {
                        @Override public void trackLoaded(AudioTrack track) {
                            connectAndPlay(guild, channel, musicManager, track);
                            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                    .setDescription("\uD83C\uDFB5 **" + track.getInfo().title + "** zur Queue hinzugefuegt (via Spotify)")
                                    .setColor(0x1DB954).build()).queue();
                        }
                        @Override public void playlistLoaded(AudioPlaylist playlist) {
                            if (!playlist.getTracks().isEmpty()) {
                                AudioTrack track = playlist.getTracks().get(0);
                                connectAndPlay(guild, channel, musicManager, track);
                                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                        .setDescription("\uD83C\uDFB5 **" + track.getInfo().title + "** zur Queue hinzugefuegt (via Spotify)")
                                        .setColor(0x1DB954).build()).queue();
                            }
                        }
                        @Override public void noMatches() { event.getHook().sendMessage("Song nicht auf YouTube gefunden.").queue(); }
                        @Override public void loadFailed(FriendlyException e) { event.getHook().sendMessage("Fehler: " + e.getMessage()).queue(); }
                    });
                } else {
                    final int[] loaded = {0};
                    final int[] failed = {0};
                    final int total = searches.size();
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription("\uD83C\uDFB5 Lade **" + total + " Songs** von Spotify...")
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
                                    String msg = "✅ **" + loaded[0] + "/" + total + " Songs** von Spotify geladen";
                                    if (failed[0] > 0) msg += " (" + failed[0] + " nicht gefunden)";
                                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                                            .setDescription(msg).setColor(0x1DB954).build()).queue();
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                event.getHook().sendMessage("Spotify-Fehler: " + e.getMessage()).queue();
            }
        });
    }

    private void handleRadio(SlashCommandInteractionEvent event) {
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("Du musst in einem Voice-Channel sein!").setEphemeral(true).queue();
            return;
        }
        String key = event.getOption("sender").getAsString().toLowerCase();
        String[] station = RADIO_STATIONS.get(key);
        if (station == null) {
            event.reply("Unbekannter Sender!").setEphemeral(true).queue();
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
                        .setDescription("\uD83D\uDCFB **" + station[0] + "** gestartet")
                        .setColor(0xEB459E).build()).queue();
            }
            @Override public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    connectAndPlay(guild, channel, musicManager, playlist.getTracks().get(0), true);
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription("\uD83D\uDCFB **" + station[0] + "** gestartet")
                            .setColor(0xEB459E).build()).queue();
                }
            }
            @Override public void noMatches() { event.getHook().sendMessage("Radio-Stream nicht erreichbar.").queue(); }
            @Override public void loadFailed(FriendlyException e) { event.getHook().sendMessage("Fehler: " + e.getMessage()).queue(); }
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
            String ownerId = MusicBot.CONFIG.getProperty("bot.owner.id", "").trim();
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
                Guild guild = jda != null ? jda.getGuildById(guildId) : null;
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
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack next = manager.scheduler.getQueue().peek();
        boolean nonstop = nonstopGuilds.contains(event.getGuild().getIdLong());
        manager.scheduler.skip();
        EmbedBuilder embed = new EmbedBuilder().setColor(0x5865F2);
        if (next != null) {
            embed.setDescription("⏭ Song uebersprungen\n\uD83C\uDFB6 Jetzt: **" + next.getInfo().title + "**");
        } else if (nonstop) {
            embed.setDescription("⏭ Song uebersprungen — \uD83D\uDD25 lade naechsten Random-Track...");
        } else {
            embed.setDescription("⏭ Song uebersprungen — Queue ist leer");
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
                .setDescription("⏹\uFE0F Musik gestoppt und Queue geleert\n💤 Nonstop startet in 2 Min wenn nichts gespielt wird")
                .setColor(0xED4245).build()).queue();
    }

    private void handlePause(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setPaused(true);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = "⏸\uFE0F Pausiert";
        if (track != null) desc += " — **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0xFEE75C).build()).queue();
    }

    private void handleResume(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setPaused(false);
        AudioTrack track = manager.player.getPlayingTrack();
        String desc = "▶\uFE0F Fortgesetzt";
        if (track != null) desc += " — **" + track.getInfo().title + "**";
        event.replyEmbeds(new EmbedBuilder().setDescription(desc).setColor(0x57F287).build()).queue();
    }

    private static final int QUEUE_PAGE_SIZE = 10;

    private void handleQueue(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        AudioTrack current = manager.player.getPlayingTrack();

        if (tracks.isEmpty() && current == null) {
            event.reply("Die Queue ist leer").queue();
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(tracks.size() / (double) QUEUE_PAGE_SIZE));
        event.replyEmbeds(buildQueueEmbed(tracks, current, manager, 0, totalPages).build())
                .addComponents(ActionRow.of(
                        Button.secondary("queue_prev_0", Emoji.fromUnicode("◀️")).withDisabled(true),
                        Button.secondary("queue_page", "Seite 1/" + totalPages).withDisabled(true),
                        Button.secondary("queue_next_0", Emoji.fromUnicode("▶️")).withDisabled(totalPages <= 1)
                )).queue();
    }

    private EmbedBuilder buildQueueEmbed(List<AudioTrack> tracks, AudioTrack current, GuildMusicManager manager, int page, int totalPages) {
        StringBuilder sb = new StringBuilder();

        if (current != null && page == 0) {
            sb.append("\uD83C\uDFB6 **Spielt gerade:**\n")
              .append("`").append(formatTime(current.getPosition())).append(" / ").append(formatTime(current.getDuration())).append("` ")
              .append(current.getInfo().title).append("\n\n");
        }

        if (!tracks.isEmpty()) {
            sb.append("**Warteschlange:**\n");
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
            case TRACK -> "  |  \uD83D\uDD02 Song Repeat";
            case QUEUE -> "  |  \uD83D\uDD01 Queue Repeat";
        };

        return new EmbedBuilder()
                .setTitle("\uD83D\uDCCB Warteschlange")
                .setDescription(sb.toString())
                .setFooter(tracks.size() + " Songs in der Queue  |  Gesamtdauer: " + formatTimeLong(totalMs) + repeatLabel)
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
            event.reply("Gerade spielt nichts").queue();
            return;
        }

        cancelNpUpdate(guildId);

        EmbedBuilder embed = track != null
                ? buildNowPlayingEmbed(track, manager)
                : buildLoadingEmbed();
        event.replyEmbeds(embed.build())
                .addComponents(npButtons(manager))
                .queue(hook -> startNpAutoUpdate(guildId, manager, event.getHook()));
    }

    private EmbedBuilder buildLoadingEmbed() {
        return new EmbedBuilder()
                .setTitle("\uD83C\uDFB5 Now Playing")
                .setDescription("\uD83D\uDD25 **Nonstop-Modus** — lade naechsten Random-Track...")
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
                        ? buildNowPlayingEmbed(current, manager)
                        : buildLoadingEmbed();
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

    private EmbedBuilder buildNowPlayingEmbed(AudioTrack track, GuildMusicManager manager) {
        long pos = track.getPosition();
        long dur = track.getDuration();
        long remaining = dur - pos;
        String position = formatTime(pos) + " / " + formatTime(dur);
        String progressBar = buildProgressBar(pos, dur);
        String repeatLabel = switch (manager.scheduler.getRepeatMode()) {
            case OFF -> "";
            case TRACK -> "  |  \uD83D\uDD02 Song";
            case QUEUE -> "  |  \uD83D\uDD01 Queue";
        };

        String status = manager.player.isPaused() ? "⏸\uFE0F Pausiert" : "\uD83D\uDD0A Vol: " + manager.player.getVolume() + "%";

        return new EmbedBuilder()
                .setTitle("\uD83C\uDFB5 Now Playing")
                .setDescription("**" + track.getInfo().title + "**\n" +
                        track.getInfo().author + "\n\n" +
                        progressBar + "\n" +
                        "`" + position + "`  ⏳ `-" + formatTime(remaining) + "`" + repeatLabel + "\n\n" +
                        status)
                .setColor(0x5865F2)
                .setThumbnail("https://img.youtube.com/vi/" + extractVideoId(track.getInfo().uri) + "/0.jpg");
    }

    private String buildProgressBar(long position, long duration) {
        int total = 20;
        int filled = duration > 0 ? (int) (position * total / duration) : 0;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i == filled) bar.append("\uD83D\uDD18");
            else if (i < filled) bar.append("▬");
            else bar.append("▬");
        }
        return bar.toString();
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
                    event.editMessageEmbeds(buildNowPlayingEmbed(t, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                }
            }
            case "np_pause" -> {
                manager.player.setPaused(!manager.player.isPaused());
                track = manager.player.getPlayingTrack();
                if (track != null) {
                    event.editMessageEmbeds(buildNowPlayingEmbed(track, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else {
                    event.reply("Gerade spielt nichts").setEphemeral(true).queue();
                }
            }
            case "np_skip" -> {
                manager.scheduler.skip();
                AudioTrack next = manager.player.getPlayingTrack();
                if (next != null) {
                    event.editMessageEmbeds(buildNowPlayingEmbed(next, manager).build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else if (nonstopGuilds.contains(guildId)) {
                    event.editMessageEmbeds(buildLoadingEmbed().build())
                            .setComponents(npButtons(manager)).queue();
                    startNpAutoUpdate(guildId, manager, event.getHook());
                } else {
                    cancelNpUpdate(guildId);
                    event.editMessage("⏭ Uebersprungen — Queue ist leer").setComponents().setEmbeds().queue();
                }
            }
            case "np_stop" -> {
                cancelNpUpdate(guildId);
                nonstopGuilds.remove(guildId);
                manager.scheduler.stop();
                installNonstopIdleHandler(guildId, manager);
                scheduleAutoNonstop(guildId, manager);
                event.editMessage("⏹️ Musik gestoppt — 💤 Nonstop startet in 2 Min").setComponents().setEmbeds().queue();
            }
        }
    }

    private void handleQueueButton(ButtonInteractionEvent event) {
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

        event.editMessageEmbeds(buildQueueEmbed(tracks, current, manager, currentPage, totalPages).build())
                .setComponents(ActionRow.of(
                        Button.secondary("queue_prev_" + currentPage, Emoji.fromUnicode("◀️")).withDisabled(currentPage == 0),
                        Button.secondary("queue_page", "Seite " + (currentPage + 1) + "/" + totalPages).withDisabled(true),
                        Button.secondary("queue_next_" + currentPage, Emoji.fromUnicode("▶️")).withDisabled(currentPage >= totalPages - 1)
                )).queue();
    }

    private void handleVolume(SlashCommandInteractionEvent event) {
        int vol = event.getOption("vol").getAsInt();
        if (vol < 0 || vol > 100) {
            event.reply("Lautstaerke muss zwischen 0 und 100 sein").setEphemeral(true).queue();
            return;
        }
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        manager.player.setVolume(vol);
        String bar = buildVolumeBar(vol);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription("\uD83D\uDD0A Lautstaerke: **" + vol + "%**\n" + bar)
                .setColor(0x5865F2).build()).queue();
    }

    private String buildVolumeBar(int vol) {
        int filled = vol / 5;
        return "▬".repeat(filled) + "\uD83D\uDD18" + "▬".repeat(20 - filled);
    }

    private void handleJoin(SlashCommandInteractionEvent event) {
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("Du musst in einem Voice-Channel sein!").setEphemeral(true).queue();
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
                .setDescription("\uD83D\uDD0A Joined **" + channel.getName() + "**")
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
                .setDescription("\uD83D\uDC4B Tschuess!")
                .setColor(0xED4245).build()).queue();
    }

    private void handleRepeat(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        String mode = event.getOption("mode").getAsString().toUpperCase();
        try {
            TrackScheduler.RepeatMode repeatMode = TrackScheduler.RepeatMode.valueOf(mode);
            manager.scheduler.setRepeatMode(repeatMode);
            String label = switch (repeatMode) {
                case OFF -> "➡\uFE0F Repeat: **Aus**";
                case TRACK -> "\uD83D\uDD02 Repeat: **Song wiederholen**";
                case QUEUE -> "\uD83D\uDD01 Repeat: **Queue wiederholen**";
            };
            event.replyEmbeds(new EmbedBuilder().setDescription(label).setColor(0x5865F2).build()).queue();
        } catch (IllegalArgumentException e) {
            event.reply("Ungueltiger Modus! Nutze: `off`, `track` oder `queue`").setEphemeral(true).queue();
        }
    }

    private void handleShuffle(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        if (manager.scheduler.getQueue().isEmpty()) {
            event.reply("Die Queue ist leer").setEphemeral(true).queue();
            return;
        }
        manager.scheduler.shuffle();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription("\uD83D\uDD00 Queue wurde gemischt! (" + manager.scheduler.getQueue().size() + " Songs)")
                .setColor(0x5865F2).build()).queue();
    }

    private void handleMove(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int from = event.getOption("von").getAsInt();
        int to = event.getOption("nach").getAsInt();
        int size = manager.scheduler.getQueue().size();
        if (from < 1 || from > size || to < 1 || to > size) {
            event.reply("Ungueltige Position! Queue hat **" + size + "** Songs.").setEphemeral(true).queue();
            return;
        }
        List<AudioTrack> tracks = new ArrayList<>(manager.scheduler.getQueue());
        String title = tracks.get(from - 1).getInfo().title;
        if (manager.scheduler.moveInQueue(from - 1, to - 1)) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription("↕️ **" + title + "** von Position " + from + " nach " + to + " verschoben")
                    .setColor(0x5865F2).build()).queue();
        } else {
            event.reply("Konnte Song nicht verschieben").setEphemeral(true).queue();
        }
    }

    private void handleSkipTo(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int pos = event.getOption("position").getAsInt();
        int size = manager.scheduler.getQueue().size();
        if (pos < 1 || pos > size) {
            event.reply("Ungueltige Position! Queue hat **" + size + "** Songs.").setEphemeral(true).queue();
            return;
        }
        AudioTrack track = manager.scheduler.skipTo(pos - 1);
        if (track != null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription("⏭ Uebersprungen zu **#" + pos + "**: **" + track.getInfo().title + "**")
                    .setColor(0x5865F2).build()).queue();
        } else {
            event.reply("Konnte nicht springen").setEphemeral(true).queue();
        }
    }

    private void handleSave(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();
        if (track == null) {
            event.reply("Gerade spielt nichts").setEphemeral(true).queue();
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83D\uDCBE Gespeicherter Song")
                .setDescription("**" + track.getInfo().title + "**\n" +
                        track.getInfo().author + "\n\n" +
                        "\uD83D\uDD17 [Link oeffnen](" + track.getInfo().uri + ")")
                .setThumbnail("https://img.youtube.com/vi/" + extractVideoId(track.getInfo().uri) + "/0.jpg")
                .setColor(0x57F287);
        event.getUser().openPrivateChannel().queue(
                dm -> {
                    dm.sendMessageEmbeds(embed.build()).queue(
                            success -> event.reply("💌 Song wurde dir per DM geschickt!").setEphemeral(true).queue(),
                            fail -> event.reply("Konnte dir keine DM senden. Aktiviere DMs von Server-Mitgliedern!").setEphemeral(true).queue()
                    );
                },
                fail -> event.reply("Konnte dir keine DM senden.").setEphemeral(true).queue()
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
                        .setDescription("🚫 **Auto-Nonstop deaktiviert** — Bot startet keine Musik mehr nach 2 Min Idle")
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
                        .setDescription("✅ **Auto-Nonstop aktiviert** — Bot spielt nach 2 Min Idle automatisch Musik")
                        .setColor(0x57F287).build()).queue();
                return;
            }
        }

        if (nonstopGuilds.contains(guildId)) {
            nonstopGuilds.remove(guildId);
            installNonstopIdleHandler(guildId, musicManager);
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription("💤 **Nonstop-Modus deaktiviert**")
                    .setColor(0xED4245).build()).queue();
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("Du musst in einem Voice-Channel sein!").setEphemeral(true).queue();
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
                .setDescription("🔥 **Nonstop-Modus aktiviert** — random Tekk, Techno, Uptempo & Co.")
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

        playerManager.loadItem(query, new AudioLoadResultHandler() {
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
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        String filter = event.getOption("preset").getAsString().toLowerCase();
        manager.applyFilter(filter);
        String label = switch (filter) {
            case "bassboost" -> "\uD83D\uDD0A **Bassboost** aktiviert";
            case "treble" -> "🎵 **Treble Boost** aktiviert";
            case "pop" -> "🎤 **Pop** Equalizer aktiviert";
            case "rock" -> "🤘 **Rock** Equalizer aktiviert";
            default -> "➡️ Filter **deaktiviert**";
        };
        event.replyEmbeds(new EmbedBuilder()
                .setDescription(label)
                .setColor(0x5865F2).build()).queue();
    }

    private void handleSeek(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        AudioTrack track = manager.player.getPlayingTrack();
        if (track == null) {
            event.reply("Gerade spielt nichts").setEphemeral(true).queue();
            return;
        }
        String input = event.getOption("time").getAsString().trim();
        long ms = parseTimeToMs(input);
        if (ms < 0) {
            event.reply("Ungueltiges Format! Nutze z.B. `1:30` oder `90` (Sekunden)").setEphemeral(true).queue();
            return;
        }
        if (ms > track.getDuration()) ms = track.getDuration();
        track.setPosition(ms);
        event.replyEmbeds(new EmbedBuilder()
                .setDescription("⏩ Gesprungen zu **" + formatTime(ms) + "** / " + formatTime(track.getDuration()))
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
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int pos = event.getOption("position").getAsInt();
        if (pos < 1 || pos > manager.scheduler.getQueue().size()) {
            event.reply("Ungueltige Position! Queue hat **" + manager.scheduler.getQueue().size() + "** Songs.").setEphemeral(true).queue();
            return;
        }
        AudioTrack removed = manager.scheduler.removeFromQueue(pos - 1);
        if (removed != null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setDescription("🗑️ **" + removed.getInfo().title + "** aus der Queue entfernt (Position " + pos + ")")
                    .setColor(0xED4245).build()).queue();
        } else {
            event.reply("Konnte Song nicht entfernen").setEphemeral(true).queue();
        }
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        GuildMusicManager manager = getGuildMusic(event.getGuild());
        int size = manager.scheduler.getQueue().size();
        if (size == 0) {
            event.reply("Die Queue ist bereits leer").setEphemeral(true).queue();
            return;
        }
        manager.scheduler.clearQueue();
        event.replyEmbeds(new EmbedBuilder()
                .setDescription("🗑️ **" + size + " Songs** aus der Queue entfernt")
                .setColor(0xED4245).build()).queue();
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83C\uDFB5 Music Bot — Hilfe")
                .setColor(0x5865F2)
                .addField("\uD83C\uDFA7 Musik",
                        "`/play <URL/Suche>` — Song, Playlist oder Radio\n" +
                        "`/skip` — Song ueberspringen\n" +
                        "`/stop` — Musik stoppen & Queue leeren\n" +
                        "`/pause` — Pausieren\n" +
                        "`/resume` — Fortsetzen\n" +
                        "`/volume <0-100>` — Lautstaerke aendern\n" +
                        "`/radio <sender>` — Live-Radio hoeren", false)
                .addField("\uD83D\uDCCB Queue & Wiedergabe",
                        "`/queue` — Warteschlange anzeigen\n" +
                        "`/playing` — Aktueller Song mit Steuerung\n" +
                        "`/seek <Zeit>` — Im Song springen (z.B. 1:30)\n" +
                        "`/skipto <Position>` — Zu Song in Queue springen\n" +
                        "`/move <von> <nach>` — Song verschieben\n" +
                        "`/repeat <off/track/queue>` — Repeat-Modus\n" +
                        "`/shuffle` — Queue mischen\n" +
                        "`/remove <Position>` — Song aus Queue entfernen\n" +
                        "`/clear` — Queue leeren\n" +
                        "`/save` — Song per DM speichern", false)
                .addField("\uD83D\uDD0A Voice & Audio",
                        "`/join` — Voice-Channel beitreten\n" +
                        "`/leave` — Voice-Channel verlassen\n" +
                        "`/filter <preset>` — Audio-Filter (Bassboost, etc.)\n" +
                        "`/nonstop` — Nonstop-Modus: random Tekk/Techno/Uptempo", false)
                .addField("\u2139\uFE0F Sonstiges",
                        "`/invite` — Bot einladen\n" +
                        "`/info` — Bot-Infos\n" +
                        "`/uptime` — Online-Zeit\n" +
                        "`/help` — Diese Hilfe", false)
                .setFooter("Quellen: YouTube, SoundCloud, Spotify, Radio  |  Playlists werden unterstuetzt!");
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleInvite(SlashCommandInteractionEvent event) {
        String botId = event.getJDA().getSelfUser().getId();
        String url = "https://discord.com/oauth2/authorize?client_id=" + botId + "&permissions=3145728&scope=bot%20applications.commands";
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83D\uDD17 Bot einladen")
                .setDescription("Klicke den Link um den Bot auf deinen Server einzuladen:\n\n" +
                        "**[Hier klicken](" + url + ")**")
                .setColor(0x57F287);
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleInfo(SlashCommandInteractionEvent event) {
        String owner = MusicBot.CONFIG.getProperty("bot.owner", "Unbekannt");
        String support = MusicBot.CONFIG.getProperty("bot.support", "Keine Angabe");

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

        int totalMembers = event.getJDA().getGuilds().stream()
                .mapToInt(Guild::getMemberCount)
                .sum();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83E\uDD16 " + event.getJDA().getSelfUser().getName())
                .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .setColor(0x5865F2)
                .addField("\uD83D\uDC51 Besitzer", owner, true)
                .addField("\uD83C\uDF10 Server", String.valueOf(event.getJDA().getGuilds().size()), true)
                .addField("\uD83D\uDC65 Mitglieder", String.valueOf(totalMembers), true)
                .addField("\u23F0 Uptime", uptimeStr, true)
                .addField("\uD83C\uDFB5 Aktive Player", activePlayers + " / " + event.getJDA().getGuilds().size(), true)
                .addField("\u200B", "\u200B", true)
                .addField("\uD83D\uDCE9 Support", support, false)
                .addField("\uD83D\uDD17 Discord", "[Server beitreten](https://discord.gg/KqngYCVJqZ)", false)
                .setFooter("Made with \u2764\uFE0F using JDA + LavaPlayer");
        event.replyEmbeds(embed.build()).queue();
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("\uD83C\uDFD3 Pong!")
                .setColor(0x5865F2)
                .addField("\uD83D\uDCE1 Gateway", gatewayPing + "ms", true);
        event.replyEmbeds(embed.build()).queue();
    }

    private final Map<Long, ScheduledFuture<?>> uptimeTasks = new HashMap<>();

    private void handleUptime(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        ScheduledFuture<?> old = uptimeTasks.remove(guildId);
        if (old != null) old.cancel(false);

        final long expireAt = System.currentTimeMillis() + 5 * 60 * 1000;
        event.replyEmbeds(buildUptimeEmbed().build()).queue(hook -> {
            ScheduledFuture<?> task = npScheduler.scheduleAtFixedRate(() -> {
                try {
                    if (System.currentTimeMillis() > expireAt) {
                        ScheduledFuture<?> t = uptimeTasks.remove(guildId);
                        if (t != null) t.cancel(false);
                        return;
                    }
                    event.getHook().editOriginalEmbeds(buildUptimeEmbed().build()).queue(null, err -> {
                        ScheduledFuture<?> t = uptimeTasks.remove(guildId);
                        if (t != null) t.cancel(false);
                    });
                } catch (Exception ignored) {}
            }, 10, 10, TimeUnit.SECONDS);
            uptimeTasks.put(guildId, task);
        });
    }

    private EmbedBuilder buildUptimeEmbed() {
        Duration uptime = Duration.between(MusicBot.START_TIME, Instant.now());

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

        return new EmbedBuilder()
                .setTitle("\u23F0 Uptime & System")
                .setColor(0x57F287)
                .addField("\u23F1\uFE0F Online seit", "**" + formatUptime(uptime) + "**", false)
                .addField("\uD83D\uDCBB CPU", cpuStr, true)
                .addField("\uD83E\uDDE9 Threads", String.valueOf(Thread.activeCount()), true)
                .addField("\u200B", "\u200B", true)
                .addField("\uD83D\uDCBE Bot RAM", usedMb + " / " + maxMb + " MB (" + ramPercent + "%)", true)
                .addField("\uD83D\uDDA5\uFE0F System RAM", sysRamStr, true);
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
