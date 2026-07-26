package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

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
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
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

    public GuildMusicManager getGuildMusic(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), id -> {
            var manager = new GuildMusicManager(playerManager);
            manager.scheduler.setJda(guild.getJDA());
            manager.scheduler.setGuildId(guild.getIdLong());
            guild.getAudioManager().setSendingHandler(manager.sendHandler);
            return manager;
        });
    }

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

    public void connectAndPlay(Guild guild, AudioChannelUnion channel, GuildMusicManager musicManager,
                               AudioTrack track) {
        connectAndPlay(guild, channel, musicManager, track, false);
    }

    public String formatTime(long ms) {
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return mins + ":" + String.format("%02d", secs);
    }

    public String formatTimeLong(long ms) {
        long hours = ms / 3600000;
        long mins = (ms % 3600000) / 60000;
        long secs = (ms % 60000) / 1000;
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m " + secs + "s";
    }

    public String extractVideoId(String url) {
        if (url == null) return "";
        if (url.contains("v=")) return url.substring(url.indexOf("v=") + 2).split("[&?]")[0];
        if (url.contains("youtu.be/")) return url.substring(url.indexOf("youtu.be/") + 9).split("[&?]")[0];
        return "";
    }

    public String buildProgressBar(long position, long duration) {
        int total = 20;
        int filled = duration > 0 ? (int) (position * total / duration) : 0;
        return "\u25AC".repeat(filled) + "\uD83D\uDD18" + "\u25AC".repeat(Math.max(0, total - filled - 1));
    }

    public String buildVolumeBar(int vol) {
        int filled = vol / 5;
        return "\u25AC".repeat(filled) + "\uD83D\uDD18" + "\u25AC".repeat(20 - filled);
    }
}
