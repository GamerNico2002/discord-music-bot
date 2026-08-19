package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.AndroidMusic;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Ios;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.MWeb;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.TvHtml5Simply;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

/** Shared state container holding all per-guild music managers, scheduled tasks, and bot-wide resources. */
public class BotContext {

    private static final Logger log = LoggerFactory.getLogger(BotContext.class);

    public final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();
    public final Set<Long> nonstopGuilds = ConcurrentHashMap.newKeySet();
    public final Set<Long> autoNonstopDisabledGuilds = ConcurrentHashMap.newKeySet();
    public final Map<Long, ScheduledFuture<?>> autoNonstopTimers = new ConcurrentHashMap<>();
    public final Map<Long, Long> lastChannelIds = new ConcurrentHashMap<>();
    public final Map<Long, ScheduledFuture<?>> npUpdateTasks = new ConcurrentHashMap<>();
    public final Map<Long, InteractionHook> npHooks = new ConcurrentHashMap<>();
    public final ScheduledExecutorService npScheduler = Executors.newScheduledThreadPool(32);

    public final SpotifyResolver spotify;
    public final String ownerId;
    public final String ownerDisplay;
    public final String supportContact;
    public final String[] nonstopGenres;
    public final String[] nonstopModifiers;
    public final java.util.Random nonstopRandom = new java.util.Random();

    /** Initializes the player manager, loads configuration, and registers audio sources. */
    public BotContext() {
        var cfg = MusicBot.CONFIG;
        spotify = new SpotifyResolver(
                cfg.getProperty("spotify.client.id", ""),
                cfg.getProperty("spotify.client.secret", ""));
        ownerId = cfg.getProperty("bot.owner.id", "").trim();
        ownerDisplay = cfg.getProperty("bot.owner", "Unbekannt");
        supportContact = cfg.getProperty("bot.support", "Keine Angabe");

        playerManager.getConfiguration().setOpusEncodingQuality(5);
        playerManager.getConfiguration().setResamplingQuality(
                com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality.MEDIUM);
        playerManager.setFrameBufferDuration(300);
        playerManager.registerSourceManager(createYoutubeSource(cfg));
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        AudioSourceManagers.registerRemoteSources(playerManager,
                com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(playerManager);

        nonstopGenres = loadCsvProperty("nonstop.genres", NonstopHandler.DEFAULT_GENRES);
        nonstopModifiers = loadCsvProperty("nonstop.modifiers", NonstopHandler.DEFAULT_MODIFIERS);
        log.info("[Nonstop] {} Genres, {} Modifiers geladen", nonstopGenres.length, nonstopModifiers.length);
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

    private static final String TOKEN_FILE = "youtube_token.txt";

    private String readTokenFromFile() {
        try {
            Path path = Paths.get(TOKEN_FILE);
            if (Files.exists(path)) {
                String token = Files.readString(path).trim();
                if (!token.isEmpty()) return token;
            }
        } catch (IOException e) {
            log.warn("[YouTube] Could not read token file: {}", e.getMessage());
        }
        return null;
    }

    private void saveTokenToFile(String token) {
        try {
            Files.writeString(Paths.get(TOKEN_FILE), token);
            log.info("[YouTube] Refresh token saved to {}", TOKEN_FILE);
        } catch (IOException e) {
            log.warn("[YouTube] Could not save token to file: {}", e.getMessage());
        }
    }

    /**
     * Creates the YouTube source manager with a remote cipher server, broad set of
     * InnerTube clients, and optional PO-token / OAuth2 authentication.
     *
     * <p>The remote cipher server (cipher.kikkia.dev) handles YouTube's JavaScript
     * signature decryption externally — this fixes the "Must find sig function" error
     * without any local software.</p>
     */
    private YoutubeAudioSourceManager createYoutubeSource(Properties cfg) {
        YoutubeSourceOptions options = new YoutubeSourceOptions()
                .setRemoteCipher("https://cipher.kikkia.dev/", "", "DiscordMusicBot");

        Client[] clients = {
                new Music(),
                new Web(),
                new MWeb(),
                new WebEmbedded(),
                new AndroidVr(),
                new AndroidMusic(),
                new TvHtml5Simply(),
                new Tv(),
                new Ios()
        };
        YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager(options, clients);

        String poToken = cfg.getProperty("youtube.po.token", "").trim();
        String visitorData = cfg.getProperty("youtube.po.visitor.data", "").trim();
        if (!poToken.isEmpty() && !visitorData.isEmpty()) {
            Web.setPoTokenAndVisitorData(poToken, visitorData);
            log.info("[YouTube] PO-Token + VisitorData konfiguriert");
        }

        String oauth = cfg.getProperty("youtube.oauth.refresh.token", "").trim();
        boolean oauthEnabled = Boolean.parseBoolean(cfg.getProperty("youtube.oauth.enabled", "false"));

        if (!oauth.isEmpty()) {
            try {
                youtube.useOauth2(oauth, true);
                log.info("[YouTube] OAuth2 mit Refresh-Token konfiguriert");
            } catch (Exception e) {
                log.warn("[YouTube] OAuth with provided token failed: {}", e.getMessage());
                log.warn("[YouTube] Continuing without OAuth — some videos may not work.");
            }
        } else {
            String savedToken = readTokenFromFile();
            if (savedToken != null) {
                try {
                    log.info("[YouTube] Loaded refresh token from {}", TOKEN_FILE);
                    youtube.useOauth2(savedToken, true);
                } catch (Exception e) {
                    log.warn("[YouTube] OAuth with saved token failed: {}", e.getMessage());
                    log.warn("[YouTube] Token may be invalid. Delete {} and restart to re-authenticate.", TOKEN_FILE);
                    log.warn("[YouTube] Continuing without OAuth — some videos may not work.");
                }
            } else if (oauthEnabled) {
                try {
                    log.info("[YouTube] Starting interactive OAuth flow...");
                    youtube.useOauth2(null, false);
                    String newToken = youtube.getOauth2RefreshToken();
                    if (newToken != null) {
                        saveTokenToFile(newToken);
                    } else {
                        log.warn("[YouTube] OAuth flow completed but refresh token could not be retrieved.");
                    }
                } catch (Exception e) {
                    log.warn("[YouTube] OAuth flow failed: {}", e.getMessage());
                    log.warn("[YouTube] Continuing without OAuth — some videos may not work.");
                }
            }
        }
        return youtube;
    }

    /**
     * Returns the {@link GuildMusicManager} for the given guild, creating one if it doesn't exist yet.
     *
     * @param guild the guild to get the music manager for
     * @return the guild's music manager, never {@code null}
     */
    public GuildMusicManager getGuildMusic(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
            var manager = new GuildMusicManager(playerManager);
            manager.scheduler.setJda(guild.getJDA());
            manager.scheduler.setGuildId(guild.getIdLong());
            guild.getAudioManager().setSendingHandler(manager.sendHandler);
            return manager;
        });
    }

    /**
     * Removes all bot state associated with the given guild (player, scheduler, timers, hooks).
     */
    public void cleanupGuild(long guildId) {
        log.info("[Cleanup] Guild {} - alle Daten entfernen", guildId);
        NonstopHandler.cancelAutoNonstop(this, guildId);
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

    /**
     * Connects to the given voice channel and starts playing or queues the track.
     *
     * @param guild       the guild to connect in
     * @param channel     the voice channel to join
     * @param musicManager the guild's music manager
     * @param track       the track to play or queue
     * @param forcePlay   if {@code true}, immediately replaces the current track
     */
    public void connectAndPlay(Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager,
                               AudioTrack track, boolean forcePlay) {
        long guildId = guild.getIdLong();
        lastChannelIds.put(guildId, channel.getIdLong());
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(musicManager.sendHandler);
        audioManager.setSelfDeafened(true);
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(channel);
        }
        NonstopHandler.installIdleHandler(this, guildId, musicManager);
        NonstopHandler.cancelAutoNonstop(this, guildId);
        if (forcePlay) {
            musicManager.scheduler.playNow(track);
        } else {
            musicManager.scheduler.queue(track);
        }
    }

    /** Overload of {@link #connectAndPlay(Guild, AudioChannelUnion, GuildMusicManager, AudioTrack, boolean)} with {@code forcePlay = false}. */
    public void connectAndPlay(Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager,
                               AudioTrack track) {
        connectAndPlay(guild, channel, musicManager, track, false);
    }

    /**
     * Formats milliseconds as {@code "mm:ss"}.
     */
    public String formatTime(long ms) {
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return mins + ":" + String.format("%02d", secs);
    }

    /**
     * Formats milliseconds in a human-readable form ({@code "Xh Ym"} or {@code "Xm Ys"}).
     */
    public String formatTimeLong(long ms) {
        long hours = ms / 3600000;
        long mins = (ms % 3600000) / 60000;
        long secs = (ms % 60000) / 1000;
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m " + secs + "s";
    }

    /**
     * Extracts the YouTube video ID from a standard YouTube URL.
     *
     * @return the video ID, or an empty string if not recognized
     */
    public String extractVideoId(String url) {
        if (url == null) return "";
        if (url.contains("v=")) return url.substring(url.indexOf("v=") + 2).split("[&?]")[0];
        if (url.contains("youtu.be/")) return url.substring(url.indexOf("youtu.be/") + 9).split("[&?]")[0];
        return "";
    }

    /**
     * Builds a text-based progress bar for the now-playing display.
     *
     * @param position current playback position in ms
     * @param duration total track duration in ms
     * @return a 20-character progress bar string
     */
    public String buildProgressBar(long position, long duration) {
        int total = 20;
        int filled = duration > 0 ? (int) (position * total / duration) : 0;
        return "\u25AC".repeat(filled) + "\uD83D\uDD18" + "\u25AC".repeat(Math.max(0, total - filled - 1));
    }

    /**
     * Builds a text-based volume bar for the volume display.
     *
     * @param vol volume level (0-100)
     * @return a 20-character volume bar string
     */
    public String buildVolumeBar(int vol) {
        int filled = vol / 5;
        return "\u25AC".repeat(filled) + "\uD83D\uDD18" + "\u25AC".repeat(20 - filled);
    }
}
